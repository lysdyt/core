package io.tetrapod.protocol.core;

// This is a code generated file.  All edits will be lost the next time code gen is run.

import io.*;
import io.tetrapod.core.rpc.*;
import io.tetrapod.core.serialize.*;
import io.tetrapod.protocol.core.TypeDescriptor;
import io.tetrapod.protocol.core.StructDescription;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings("unused")
public class RequestStat extends Structure {
   
   public static final int STRUCT_ID = 12902770;
   public static final int CONTRACT_ID = CoreContract.CONTRACT_ID;
    
   public RequestStat() {
      defaults();
   }

   public RequestStat(String name, long count, long time, int[] entities, int[] errors, int[] timeline) {
      this.name = name;
      this.count = count;
      this.time = time;
      this.entities = entities;
      this.errors = errors;
      this.timeline = timeline;
   }   
   
   /**
    * name of the request
    */
   public String name;
   
   /**
    * number of invocations
    */
   public long count;
   
   /**
    * cumulative time spent in microseconds
    */
   public long time;
   
   /**
    * top callers
    */
   public int[] entities;
   
   /**
    * top errors
    */
   public int[] errors;
   
   /**
    * histogram of calls
    */
   public int[] timeline;

   public final Structure.Security getSecurity() {
      return Security.INTERNAL;
   }

   public final void defaults() {
      name = null;
      count = 0;
      time = 0;
      entities = null;
      errors = null;
      timeline = null;
   }
   
   @Override
   public final void write(DataSource data) throws IOException {
      data.write(1, this.name);
      data.write(2, this.count);
      data.write(3, this.time);
      if (this.entities != null) data.write(4, this.entities);
      if (this.errors != null) data.write(5, this.errors);
      if (this.timeline != null) data.write(6, this.timeline);
      data.writeEndTag();
   }
   
   @Override
   public final void read(DataSource data) throws IOException {
      defaults();
      while (true) {
         int tag = data.readTag();
         switch (tag) {
            case 1: this.name = data.read_string(tag); break;
            case 2: this.count = data.read_long(tag); break;
            case 3: this.time = data.read_long(tag); break;
            case 4: this.entities = data.read_int_array(tag); break;
            case 5: this.errors = data.read_int_array(tag); break;
            case 6: this.timeline = data.read_int_array(tag); break;
            case Codec.END_TAG:
               return;
            default:
               data.skip(tag);
               break;
         }
      }
   }
   
   public final int getContractId() {
      return RequestStat.CONTRACT_ID;
   }

   public final int getStructId() {
      return RequestStat.STRUCT_ID;
   }

   public final String[] tagWebNames() {
      // Note do not use this tags in long term serializations (to disk or databases) as 
      // implementors are free to rename them however they wish.  A null means the field
      // is not to participate in web serialization (remaining at default)
      String[] result = new String[6+1];
      result[1] = "name";
      result[2] = "count";
      result[3] = "time";
      result[4] = "entities";
      result[5] = "errors";
      result[6] = "timeline";
      return result;
   }

   public final Structure make() {
      return new RequestStat();
   }

   public final StructDescription makeDescription() {
      StructDescription desc = new StructDescription();
      desc.name = "RequestStat";
      desc.tagWebNames = tagWebNames();
      desc.types = new TypeDescriptor[desc.tagWebNames.length];
      desc.types[0] = new TypeDescriptor(TypeDescriptor.T_STRUCT, getContractId(), getStructId());
      desc.types[1] = new TypeDescriptor(TypeDescriptor.T_STRING, 0, 0);
      desc.types[2] = new TypeDescriptor(TypeDescriptor.T_LONG, 0, 0);
      desc.types[3] = new TypeDescriptor(TypeDescriptor.T_LONG, 0, 0);
      desc.types[4] = new TypeDescriptor(TypeDescriptor.T_INT_LIST, 0, 0);
      desc.types[5] = new TypeDescriptor(TypeDescriptor.T_INT_LIST, 0, 0);
      desc.types[6] = new TypeDescriptor(TypeDescriptor.T_INT_LIST, 0, 0);
      return desc;
   }
}
