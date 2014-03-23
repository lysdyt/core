package io.tetrapod.protocol.core;

// This is a code generated file.  All edits will be lost the next time code gen is run.

import io.*;
import io.tetrapod.core.serialize.*;
import io.tetrapod.core.rpc.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings("unused")
public class TopicUnpublishedMessage extends Message {
   
   public static final int STRUCT_ID = 6594504;
    
   public TopicUnpublishedMessage() {
      defaults();
   }

   public TopicUnpublishedMessage(int ownerId, int topicId) {
      this.ownerId = ownerId;
      this.topicId = topicId;
   }   
   
   public int ownerId;
   public int topicId;

   public final Structure.Security getSecurity() {
      return Security.INTERNAL;
   }

   public final void defaults() {
      ownerId = 0;
      topicId = 0;
   }
   
   @Override
   public final void write(DataSource data) throws IOException {
      data.write(1, this.ownerId);
      data.write(2, this.topicId);
      data.writeEndTag();
   }
   
   @Override
   public final void read(DataSource data) throws IOException {
      defaults();
      while (true) {
         int tag = data.readTag();
         switch (tag) {
            case 1: this.ownerId = data.read_int(tag); break;
            case 2: this.topicId = data.read_int(tag); break;
            case Codec.END_TAG:
               return;
            default:
               data.skip(tag);
               break;
         }
      }
   }
   
   @Override
   public final int getStructId() {
      return TopicUnpublishedMessage.STRUCT_ID;
   }
   
   @Override
   public final void dispatch(SubscriptionAPI api) {
      if (api instanceof Handler)
         ((Handler)api).messageTopicUnpublished(this);
      else
         api.genericMessage(this);
   }
   
   public static interface Handler extends SubscriptionAPI {
      void messageTopicUnpublished(TopicUnpublishedMessage m);
   }
   
   public static Callable<Structure> getInstanceFactory() {
      return new Callable<Structure>() {
         public Structure call() { return new TopicUnpublishedMessage(); }
      };
   }
   
   public final int getContractId() {
      return TetrapodContract.CONTRACT_ID;
   }
}