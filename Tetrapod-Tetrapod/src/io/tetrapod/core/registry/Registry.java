package io.tetrapod.core.registry;

import io.tetrapod.core.*;
import io.tetrapod.core.rpc.*;
import io.tetrapod.protocol.core.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.slf4j.*;

/**
 * The global registry of all current actors in the system and their published topics and subscriptions
 * 
 * Each tetrapod service owns a shard of the registry and has a full replica of all other shards.
 */
public class Registry implements TetrapodContract.Registry.API {

   protected static final Logger                logger          = LoggerFactory.getLogger(Registry.class);

   public static final int                      MAX_PARENTS     = 0x000007FF;
   public static final int                      MAX_ID          = 0x000FFFFF;

   public static final int                      PARENT_ID_SHIFT = 20;
   public static final int                      PARENT_ID_MASK  = MAX_PARENTS << PARENT_ID_SHIFT;
   public static final int                      BOOTSTRAP_ID    = 1 << PARENT_ID_SHIFT;

   private static final String                  PARENT_ID_LOCK  = "REGISTRY.PARENT_ID.LOCK";

   /**
    * A read-write lock is used to synchronize subscriptions to the registry state, and it is a little counter-intuitive. When making write
    * operations to the registry, we grab the read lock to allow concurrent writes across the registry. When we need to send the current
    * state snapshot to another cluster member, we grab the write lock for exclusive access to send a consistent state.
    */
   public final ReadWriteLock                   lock            = new ReentrantReadWriteLock();

   /**
    * Our entityId
    */
   private int                                  parentId;

   /**
    * Our local entity id counter
    */
   private int                                  counter;

   /**
    * Maps entityId => EntityInfo
    */
   private final Map<Integer, EntityInfo>       entities        = new ConcurrentHashMap<>();

   /**
    * Maps contractId => List of EntityInfos that provide that service
    */
   private final Map<Integer, List<EntityInfo>> services        = new ConcurrentHashMap<>();

   private Storage                              storage;

   public static interface RegistryBroadcaster {
      public void broadcastRegistryMessage(Message msg);

      public void broadcastServicesMessage(Message msg);
   }

   private final RegistryBroadcaster broadcaster;

   public Registry(RegistryBroadcaster broadcaster) {
      this.broadcaster = broadcaster;
   }

   public synchronized void setParentId(int id) {
      this.parentId = id;
   }

   public synchronized int getParentId() {
      return parentId;
   }

   public Storage getStorage() {
      return storage;
   }

   public void setStorage(Storage storage) {
      this.storage = storage;
   }

   public Collection<EntityInfo> getEntities() {
      return entities.values();
   }

   public List<EntityInfo> getChildren() {
      final List<EntityInfo> children = new ArrayList<>();
      for (EntityInfo e : entities.values()) {
         if (e.parentId == parentId && e.entityId != parentId) {
            children.add(e);
         }
      }
      return children;
   }

   public List<EntityInfo> getServices() {
      final List<EntityInfo> list = new ArrayList<>();
      for (EntityInfo e : entities.values()) {
         if (e.isService()) {
            list.add(e);
         }
      }
      return list;
   }

   public EntityInfo getEntity(int entityId) {
      return entities.get(entityId);
   }

   public EntityInfo getFirstAvailableService(int contractId) {
      final List<EntityInfo> list = services.get(contractId);
      if (list != null) {
         final ListIterator<EntityInfo> li = list.listIterator();
         while (li.hasNext()) {
            final EntityInfo info = li.next();
            if (info.isAvailable()) {
               return info;
            }
         }
      }
      return null;
   }

   public EntityInfo getRandomAvailableService(int contractId) {
      final List<EntityInfo> list = services.get(contractId);
      if (list != null) {
         final List<EntityInfo> shuffled = new ArrayList<>(list);
         Collections.shuffle(shuffled);
         for (EntityInfo info : shuffled) {
            if (info != null && info.isAvailable()) {
               return info;
            }
         }
      }
      return null;
   }

   /**
    * @return a new unused ID. If we hit our local maximum, we will reset and find the next currently unused id
    */
   private synchronized int issueId() {
      while (true) {
         int id = parentId | (++counter % MAX_ID);
         if (!entities.containsKey(id)) {
            return id;
         }
      }
   }

   /**
    * Issue the next available tetrapod id
    * 
    * FIXME: This is currently _very_ unsafe, but will do until we have the robust distributed counters or locks implemented
    * 
    * FIXME: Use hazelcast counter
    */
   public synchronized int issueTetrapodId() {
      storage.getLock(PARENT_ID_LOCK).lock();
      try {
         int nextId = parentId >> PARENT_ID_SHIFT;
         while (true) {
            int id = (++nextId % MAX_PARENTS) << PARENT_ID_SHIFT;
            if (!entities.containsKey(id)) {
               return id;
            }
         }
      } finally {
         storage.getLock(PARENT_ID_LOCK).unlock();
      }
   }

   public void register(EntityInfo entity) {
      lock.readLock().lock();
      try {

         if (entity.entityId <= 0) {
            if (entity.isTetrapod()) {
               entity.entityId = issueTetrapodId();
            } else {
               entity.entityId = issueId();
            }
         }

         entities.put(entity.entityId, entity);
         if (entity.isService()) {
            // register their service in our services list
            ensureServicesList(entity.contractId).add(entity);
         }
         if (entity.parentId == parentId && entity.entityId != parentId && broadcaster != null) {
            broadcaster.broadcastRegistryMessage(new EntityRegisteredMessage(entity));
         }
         if (entity.isService()) {
            broadcaster.broadcastServicesMessage(new ServiceAddedMessage(entity));
         }
      } finally {
         lock.readLock().unlock();
      }
   }

   private void clearAllTopicsAndSubscriptions(final EntityInfo e) {
      // Unpublish all their topics
      for (Topic topic : new ArrayList<Topic>(e.getTopics())) {
         unpublish(e, topic.topicId);
      }
      // Unsubscribe from all subscriptions
      for (Topic topic : new ArrayList<Topic>(e.getSubscriptions())) {
         EntityInfo owner = getEntity(topic.ownerId);
         assert (owner != null); // bug here cleaning up topics on unreg, I think...
         if (owner != null) {
            unsubscribe(owner, topic.topicId, e.entityId, true);
         }
      }
   }

   public void unregister(final EntityInfo e) {
      lock.readLock().lock();
      try {
         clearAllTopicsAndSubscriptions(e);

         entities.remove(e.entityId);

         if (e.parentId == parentId) {
            broadcaster.broadcastRegistryMessage(new EntityUnregisteredMessage(e.entityId));
         }
         if (e.isService()) {
            broadcaster.broadcastServicesMessage(new ServiceRemovedMessage(e.entityId));
         }

         if (e.isService()) {
            List<EntityInfo> list = services.get(e.contractId);
            if (list != null)
               list.remove(e);
         }
      } finally {
         lock.readLock().unlock();
      }
   }

   public void updateStatus(final EntityInfo e, int status) {
      lock.readLock().lock();
      try {
         e.setStatus(status);
         if (e.parentId == parentId) {
            broadcaster.broadcastRegistryMessage(new EntityUpdatedMessage(e.entityId, status));
         }
         if (e.isService()) {
            broadcaster.broadcastServicesMessage(new ServiceUpdatedMessage(e.entityId, status));
         }
      } finally {
         lock.readLock().unlock();
      }
   }

   public Topic publish(int entityId) {
      lock.readLock().lock();
      try {
         final EntityInfo e = getEntity(entityId);
         if (e != null) {
            Topic topic = e.publish();
            if (e.parentId == parentId) {
               broadcaster.broadcastRegistryMessage(new TopicPublishedMessage(e.entityId, topic.topicId));
            }
            return topic;
         } else {
            logger.error("Could not find entity {}", e);
         }
      } finally {
         lock.readLock().unlock();
      }
      return null;
   }

   public boolean unpublish(EntityInfo e, int topicId) {
      lock.readLock().lock();
      try {
         final Topic topic = e.unpublish(topicId);
         if (topic != null) {
            // clean up all the subscriptions to this topic
            for (Subscriber sub : topic.getSubscribers()) {
               unsubscribe(e, topic, sub.entityId, true);
            }
            if (e.parentId == parentId) {
               broadcaster.broadcastRegistryMessage(new TopicUnpublishedMessage(e.entityId, topicId));
            }
            return true;
         }
      } finally {
         lock.readLock().unlock();
      }
      return false;
   }

   public void subscribe(final EntityInfo publisher, final int topicId, final int entityId) {
      lock.readLock().lock();
      try {
         final Topic topic = publisher.getTopic(topicId);
         if (topic != null) {
            final EntityInfo e = getEntity(entityId);
            if (e != null) {
               topic.subscribe(publisher, e, parentId);
               e.subscribe(topic);

               if (publisher.parentId == parentId) {
                  broadcaster.broadcastRegistryMessage(new TopicSubscribedMessage(topic.ownerId, topic.topicId, e.entityId));
               }
            } else {
               logger.info("Could not find subscriber {} for topic {}", entityId, topicId);
            }
         } else {
            logger.info("Could not find topic {} for {}", topicId, publisher);
         }
      } finally {
         lock.readLock().unlock();
      }
   }

   public void unsubscribe(final EntityInfo publisher, final int topicId, final int entityId, final boolean all) {
      assert (publisher != null);
      lock.readLock().lock();
      try {
         final Topic topic = publisher.getTopic(topicId);
         if (topic != null) {
            unsubscribe(publisher, topic, entityId, all);
         } else {
            logger.info("Could not find topic {} for {}", topicId, publisher);
         }
      } finally {
         lock.readLock().unlock();
      }
   }

   public void unsubscribe(final EntityInfo publisher, Topic topic, final int entityId, final boolean all) {
      assert (publisher != null);
      assert (topic != null);
      lock.readLock().lock();
      try {
         final EntityInfo e = getEntity(entityId);
         if (e != null) {
            final boolean isProxy = !e.isTetrapod() && e.parentId != parentId;
            if (topic.unsubscribe(e.entityId, e.parentId, isProxy, all)) {
               e.unsubscribe(topic);
            }
            if (publisher.parentId == parentId) {
               broadcaster.broadcastRegistryMessage(new TopicUnsubscribedMessage(publisher.entityId, topic.topicId, entityId));
            }
         } else {
            logger.info("Could not find subscriber {} for topic {}", entityId, topic.topicId);
         }
      } finally {
         lock.readLock().unlock();
      }
   }

   //////////////////////////////////////////////////////////////////////////////////////////

   @Override
   public void genericMessage(Message message, MessageContext ctx) {}

   @Override
   public void messageEntityRegistered(EntityRegisteredMessage m, MessageContext ctx) {
      // TODO: validate sender    
      if (ctx.header.fromId != parentId) {
         lock.readLock().lock();
         try {
            EntityInfo info = entities.get(m.entity.entityId);
            if (info != null) {
               info.parentId = m.entity.parentId;
               info.reclaimToken = m.entity.reclaimToken;
               info.host = m.entity.host;
               info.status = m.entity.status;
               info.build = m.entity.build;
               info.name = m.entity.name;
               info.version = m.entity.version;
               info.contractId = m.entity.contractId;
               final EntityInfo e = info;
               info.queue(new Runnable() {
                  public void run() {
                     clearAllTopicsAndSubscriptions(e);
                  }
               });
            } else {
               info = new EntityInfo(m.entity);
            }
            register(info);
         } finally {
            lock.readLock().unlock();
         }
      }
   }

   @Override
   public void messageEntityUnregistered(EntityUnregisteredMessage m, MessageContext ctx) {
      // TODO: validate sender           
      if (ctx.header.fromId != parentId) {
         final EntityInfo e = getEntity(m.entityId);
         if (e != null) {
            e.queue(new Runnable() {
               public void run() {
                  unregister(e);
               }
            });
         } else {
            logger.error("Could not find entity {} to unregister", m.entityId);
         }
      }
   }

   @Override
   public void messageEntityUpdated(final EntityUpdatedMessage m, MessageContext ctx) {
      // TODO: validate sender           
      if (ctx.header.fromId != parentId) {
         final EntityInfo e = getEntity(m.entityId);
         if (e != null) {
            e.queue(new Runnable() {
               public void run() {
                  updateStatus(e, m.status);
               }
            });
         } else {
            logger.error("Could not find entity {} to update", m.entityId);
         }
      }
   }

   @Override
   public void messageTopicPublished(final TopicPublishedMessage m, MessageContext ctx) {
      // TODO: validate sender
      if (ctx.header.fromId != parentId) {
         final EntityInfo owner = getEntity(m.ownerId);
         if (owner != null) {
            owner.queue(new Runnable() {
               public void run() {
                  owner.nextTopicId();// increment our topic counter
                  owner.publish(m.topicId);
               }
            }); // TODO: kick()
         } else {
            logger.info("Could not find publisher entity {}", m.ownerId);
         }
      }
   }

   @Override
   public void messageTopicUnpublished(final TopicUnpublishedMessage m, MessageContext ctx) {
      // TODO: validate sender
      if (ctx.header.fromId != parentId) {
         final EntityInfo owner = getEntity(m.ownerId);
         if (owner != null) {
            owner.queue(new Runnable() {
               public void run() {
                  final Topic topic = owner.unpublish(m.topicId);
                  if (topic == null) {
                     logger.info("Could not find topic {} for entity {}", m.topicId, m.ownerId);
                  }
               }
            }); // TODO: kick()
         } else {
            logger.info("Could not find publisher entity {}", m.ownerId);
         }
      }
   }

   @Override
   public void messageTopicSubscribed(final TopicSubscribedMessage m, MessageContext ctx) {
      // TODO: validate sender 
      if (ctx.header.fromId != parentId) {
         final EntityInfo owner = getEntity(m.ownerId);
         if (owner != null) {
            owner.queue(new Runnable() {
               public void run() {
                  subscribe(owner, m.topicId, m.entityId);
               }
            }); // TODO: kick() 
         } else {
            logger.info("Could not find publisher entity {}", m.ownerId);
         }
      }
   }

   @Override
   public void messageTopicUnsubscribed(final TopicUnsubscribedMessage m, MessageContext ctx) {
      if (ctx.header.fromId != parentId) {
         // TODO: validate sender           
         final EntityInfo owner = getEntity(m.ownerId);
         if (owner != null) {
            owner.queue(new Runnable() {
               public void run() {
                  unsubscribe(owner, m.topicId, m.entityId, false);
               }
            }); // TODO: kick()
         } else {
            logger.info("Could not find publisher entity {}", m.ownerId);
         }
      }
   }

   @Override
   public void messageEntityListComplete(EntityListCompleteMessage m, MessageContext ctx) {
      logger.info("====================== SYNCED {} ======================", ctx.header.fromId);
   }

   //////////////////////////////////////////////////////////////////////////////////////////

   public void sendRegistryState(final Session session, final int toEntityId, final int topicId) {
      lock.writeLock().lock();
      try {
         // Sends all current entities -- ourselves, and our children
         final EntityInfo me = getEntity(parentId);
         session.sendMessage(new EntityRegisteredMessage(me), toEntityId, topicId);
         for (Topic t : me.getTopics()) {
            session.sendMessage(new TopicPublishedMessage(me.entityId, t.topicId), toEntityId, topicId);
         }

         for (EntityInfo e : getChildren()) {
            session.sendMessage(new EntityRegisteredMessage(e), toEntityId, topicId);
            for (Topic t : e.getTopics()) {
               session.sendMessage(new TopicPublishedMessage(e.entityId, t.topicId), toEntityId, topicId);
            }
         }
         // send topic info
         // OPTIMIZE: could be optimized greatly with custom messages, but this is very simple
         sendSubscribers(me, session, toEntityId, topicId);
         for (EntityInfo e : getChildren()) {
            sendSubscribers(e, session, toEntityId, topicId);
         }
         session.sendMessage(new EntityListCompleteMessage(), toEntityId, topicId);
      } finally {
         lock.writeLock().unlock();
      }
   }

   private void sendSubscribers(final EntityInfo e, final Session session, final int toEntityId, final int topicId) {
      for (Topic t : e.getTopics()) {
         for (Subscriber s : t.getSubscribers()) {
            session.sendMessage(new TopicSubscribedMessage(t.ownerId, t.topicId, s.entityId), toEntityId, topicId);
         }
      }
   }

   //////////////////////////////////////////////////////////////////////////////////////////

   public synchronized void logStats() {
      List<EntityInfo> list = new ArrayList<>(entities.values());
      Collections.sort(list);
      logger.info("========================== TETRAPOD CLUSTER REGISTRY ============================");
      for (EntityInfo e : list) {
         logger.info(String.format(" 0x%08X 0x%08X %-15s status=%08X topics=%d subscriptions=%d", e.parentId, e.entityId, e.name, e.status,
               e.getNumTopics(), e.getNumSubscriptions()));
      }
      logger.info("=================================================================================\n");
   }

   private List<EntityInfo> ensureServicesList(int contractId) {
      List<EntityInfo> list = services.get(contractId);
      if (list == null) {
         list = new CopyOnWriteArrayList<>();
         services.put(contractId, list);
      }
      return list;
   }

   public void setGone(EntityInfo e) {
      lock.readLock().lock();
      try {
         updateStatus(e, e.status | Core.STATUS_GONE);
         e.setSession(null);
         if (e.isTetrapod()) {
            for (EntityInfo child : entities.values()) {
               if (child.parentId == e.entityId) {
                  updateStatus(child, child.status | Core.STATUS_GONE);
               }
            }
         }
      } finally {
         lock.readLock().unlock();
      }
   }

}