package io.tetrapod.core.logging;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;
import org.slf4j.*;

import io.netty.buffer.ByteBuf;
import io.tetrapod.core.*;
import io.tetrapod.core.rpc.*;
import io.tetrapod.core.serialize.StructureAdapter;
import io.tetrapod.core.serialize.datasources.IOStreamDataSource;
import io.tetrapod.core.utils.Util;
import io.tetrapod.protocol.core.*;
import io.tetrapod.protocol.raft.AppendEntriesRequest;

/**
 * Buffers and writes binary logs
 */
public class CommsLogger {

   private static final Logger       logger           = LoggerFactory.getLogger(CommsLogger.class);
   private static final Logger       commsLog         = LoggerFactory.getLogger("comms");

   private static final boolean      LOG_TEXT_CONSOLE = true;

   private static final int          LOG_FILE_VERSION = 1;

   private static CommsLogger        SINGLETON;

   private DataOutputStream          out;
   private LinkedList<CommsLogEntry> buffer           = new LinkedList<>();
   private volatile boolean          shutdown         = false;
   private LocalDateTime             logOpenTime;
   private File                      currentLogFile;

   public CommsLogger() throws IOException {
      Thread t = new Thread(() -> writerThread(), "CommsLogWriter");
      t.start();
   }

   public void doShutdown() {
      shutdown = true;
      try {
         closeLogFile();
      } catch (IOException e) {
         logger.error(e.getMessage(), e);
      }
   }

   private void writerThread() {
      while (!shutdown) {
         // starts a new log file every hour
         final LocalDateTime time = LocalDateTime.now();
         if (logOpenTime == null || time.getHour() != logOpenTime.getHour() || time.getDayOfYear() != logOpenTime.getDayOfYear()) {
            try {
               openLogFile();
            } catch (IOException e) {
               logger.error(e.getMessage(), e);
            }
         }

         while (!buffer.isEmpty()) {
            CommsLogEntry entry = null;
            synchronized (buffer) {
               entry = buffer.poll();
            }
            try {
               entry.write(out);
            } catch (IOException e) {
               logger.error(e.getMessage(), e);
            }
         }
         try {
            out.flush();
         } catch (IOException e) {
            logger.error(e.getMessage(), e);
         }

         Util.sleep(100);
      }
   }

   private void openLogFile() throws IOException {
      closeLogFile();
      if (currentLogFile != null) {
         archiveLogFile();
      }
      File logs = new File(Util.getProperty("tetrapod.logs.comms", "logs/comms/"));
      LocalDate date = LocalDate.now();
      File dir = new File(logs, String.format("%d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
      dir.mkdirs();
      currentLogFile = new File(dir, "current.log");
      out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(currentLogFile, false)));
      List<StructDescription> defs = StructureFactory.getAllKnownStructures();
      out.writeInt(LOG_FILE_VERSION);
      out.writeInt(defs.size());

      IOStreamDataSource data = IOStreamDataSource.forWriting(out);
      for (StructDescription def : defs) {
         def.write(data);
      }

      logOpenTime = LocalDateTime.now();
   }

   private void closeLogFile() throws IOException {
      if (out != null) {
         out.close();
      }
   }

   private void archiveLogFile() throws IOException {
      // rename and gzip/upload
      final File file = new File(currentLogFile.getParent(), String.format("%d-%02d-%02d_%02d.comms", logOpenTime.getYear(),
            logOpenTime.getMonthValue(), logOpenTime.getDayOfMonth(), logOpenTime.getHour()));
      currentLogFile.renameTo(file);
      final File gzFile = new File(currentLogFile.getParent(), String.format("%d-%02d-%02d_%02d.comms.gz", logOpenTime.getYear(),
            logOpenTime.getMonthValue(), logOpenTime.getDayOfMonth(), logOpenTime.getHour()));
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
         @SuppressWarnings("unused")
         int ver = in.readInt();
         int defCount = in.readInt();

         List<StructDescription> defs = new ArrayList<>();
         IOStreamDataSource data = IOStreamDataSource.forReading(in);
         for (int i = 0; i < defCount; i++) {
            StructDescription def = new StructDescription();
            def.read(data);
            defs.add(def);
         }

         try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(gzFile))))) {
            out.writeInt(LOG_FILE_VERSION);
            out.writeInt(defs.size());
            data = IOStreamDataSource.forWriting(out);
            for (StructDescription def : defs) {
               def.write(data);
            }

            while (true) {
               CommsLogEntry.read(in).write(out);
            }
         } catch (IOException e) {}
      }

   }

   public void append(CommsLogEntry entry) {
      synchronized (buffer) {
         buffer.add(entry);
      }
   }

   public static void append(Session session, boolean sending, MessageHeader header, ByteBuf in) {
      if (!commsLogIgnore(header.structId)) {
         byte[] data = new byte[in.readableBytes()];
         in.getBytes(in.readerIndex(), data);
         SINGLETON.append(new CommsLogEntry(new CommsLogHeader(System.currentTimeMillis(), LogHeaderType.MESSAGE, sending), header, data));
         if (commsLog.isDebugEnabled()) {
            boolean isBroadcast = header.toChildId == 0 && header.topicId != 1;
            commsLog(session, "[%s] %s Message: %s (to %d.%d t%d f%d)", isBroadcast ? "B" : "M", sending ? "->" : "<-", getNameFor(header),
                  header.toParentId, header.toChildId, header.topicId, header.flags);
         }
      }
   }

   public static void append(Session session, boolean sending, MessageHeader header, Message msg) {
      if (!commsLogIgnore(header.structId)) {
         SINGLETON.append(new CommsLogEntry(new CommsLogHeader(System.currentTimeMillis(), LogHeaderType.MESSAGE, sending), header, msg));
         if (commsLog.isDebugEnabled()) {
            boolean isBroadcast = header.toChildId == 0 && header.topicId != 1;
            commsLog(session, "[%s] %s Message: %s (to %d.%d t%d f%d)", isBroadcast ? "B" : "M", sending ? "->" : "<-", getNameFor(header),
                  header.toParentId, header.toChildId, header.topicId, header.flags);
         }
      }
   }

   public static boolean append(Session session, boolean sending, RequestHeader header, ByteBuf in) {
      if (!commsLogIgnore(header.structId)) {
         byte[] data = new byte[in.readableBytes()];
         in.getBytes(in.readerIndex(), data);
         SINGLETON.append(new CommsLogEntry(new CommsLogHeader(System.currentTimeMillis(), LogHeaderType.REQUEST, sending), header, data));
         if (commsLog.isDebugEnabled()) {
            commsLog(session, "%016X [%d] %s %s (from %d.%d)", header.contextId, header.requestId, sending ? "->" : "<-",
                  StructureFactory.getName(header.contractId, header.structId), header.fromParentId, header.fromChildId);
         }
         return true;
      }
      return false;
   }

   public static boolean append(Session session, boolean sending, RequestHeader header, Structure req) {
      if (!commsLogIgnore(header.structId)) {
         SINGLETON.append(new CommsLogEntry(new CommsLogHeader(System.currentTimeMillis(), LogHeaderType.REQUEST, sending), header, req));
         if (commsLog.isDebugEnabled()) {
            commsLog(session, "%016X [%d] %s %s (from %d.%d)", header.contextId, header.requestId, sending ? "->" : "<-", req.dump(),
                  header.fromParentId, header.fromChildId);
         }
         return true;
      }
      return false;
   }

   public static boolean append(Session session, boolean sending, ResponseHeader header, ByteBuf in) {
      if (!commsLogIgnore(header.structId)) {
         byte[] data = new byte[in.readableBytes()];
         in.getBytes(in.readerIndex(), data);
         SINGLETON.append(new CommsLogEntry(new CommsLogHeader(System.currentTimeMillis(), LogHeaderType.RESPONSE, sending), header, data));
         if (commsLog.isDebugEnabled()) {
            commsLog(session, "%016X [%d] %s %s", header.contextId, header.requestId, sending ? "->" : "<-", getNameFor(header));
         }
         return true;
      }
      return false;
   }

   public static boolean append(Session session, boolean sending, ResponseHeader header, Structure res) {
      if (!commsLogIgnore(header.structId)) {
         SINGLETON.append(new CommsLogEntry(new CommsLogHeader(System.currentTimeMillis(), LogHeaderType.RESPONSE, sending), header, res));
         if (commsLog.isDebugEnabled()) {
            commsLog(session, "%016X [%d] %s %s", header.contextId, header.requestId, sending ? "->" : "<-", res.dump());
         }
         return true;
      }
      return false;
   }

   public static void init() throws IOException {
      SINGLETON = new CommsLogger();
   }

   public static boolean commsLog(Session ses, String format, Object... args) {
      if (commsLog.isDebugEnabled()) {
         final String str = String.format("%s:%d ", ses.getClass().getSimpleName().substring(0, 4), ses.getSessionNum())
               + String.format(format, args);
         commsLog.debug(str);
         if (LOG_TEXT_CONSOLE) {
            logger.debug(str);
         }
      }
      return true;
   }

   public static String getNameFor(MessageHeader header) {
      return StructureFactory.getName(header.contractId, header.structId);
   }

   public static String getNameFor(ResponseHeader header) {
      return StructureFactory.getName(header.contractId, header.structId);
   }

   public static boolean commsLogIgnore(Structure struct) {
      return commsLogIgnore(struct.getStructId());
   }

   public static boolean commsLogIgnore(int structId) {
      if (commsLog.isTraceEnabled())
         return false;
      switch (structId) {
         case ServiceLogsRequest.STRUCT_ID:
         case ServiceStatsMessage.STRUCT_ID:
         case DummyRequest.STRUCT_ID:
         case AppendEntriesRequest.STRUCT_ID:
         case RaftStatsRequest.STRUCT_ID:
         case RaftStatsResponse.STRUCT_ID:
            return true;
      }
      return !commsLog.isDebugEnabled();
   }

   public static void shutdown() {
      SINGLETON.doShutdown();
   }

   @Test
   public void test() throws FileNotFoundException, IOException {
      File file = new File("/Users/adavidson/workspace/tetrapod/core/Tetrapod-Web/logs/comms/2016-09-01/current.log");
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
         @SuppressWarnings("unused")
         int ver = in.readInt();
         int defCount = in.readInt();

         IOStreamDataSource data = IOStreamDataSource.forReading(in);
         for (int i = 0; i < defCount; i++) {
            StructDescription def = new StructDescription();
            def.read(data);
            StructureFactory.add(new StructureAdapter(def));
         }

         while (true) {
            CommsLogEntry e = CommsLogEntry.read(in);
            System.out.println(e.header.timestamp + " " + e.header.type + " : " + e.struct.dump());
         }
      } catch (IOException e) {}
   }

}