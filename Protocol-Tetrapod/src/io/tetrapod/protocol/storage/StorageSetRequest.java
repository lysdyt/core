package io.tetrapod.protocol.storage;

// This is a code generated file.  All edits will be lost the next time code gen is run.

import io.*;
import io.tetrapod.core.rpc.*;
import io.tetrapod.protocol.core.Admin;
import io.tetrapod.core.serialize.*;
import io.tetrapod.protocol.core.TypeDescriptor;
import io.tetrapod.protocol.core.StructDescription;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings("all")
public class StorageSetRequest extends Request {

   public static final int STRUCT_ID = 12758845;
   public static final int CONTRACT_ID = StorageContract.CONTRACT_ID;
   public static final int SUB_CONTRACT_ID = StorageContract.SUB_CONTRACT_ID;

   public StorageSetRequest() {
      defaults();
   }

   public StorageSetRequest(String key, String value) {
      this.key = key;
      this.value = value;
   }   

   public String key;
   public String value;

   public final Structure.Security getSecurity() {
      return Security.INTERNAL;
   }

   public final void defaults() {
      key = null;
      value = null;
   }
   
   @Override
   public final void write(DataSource data) throws IOException {
      data.write(1, this.key);
      data.write(2, this.value);
      data.writeEndTag();
   }
   
   @Override
   public final void read(DataSource data) throws IOException {
      defaults();
      while (true) {
         int tag = data.readTag();
         switch (tag) {
            case 1: this.key = data.read_string(tag); break;
            case 2: this.value = data.read_string(tag); break;
            case Codec.END_TAG:
               return;
            default:
               data.skip(tag);
               break;
         }
      }
   }
   
   public final int getContractId() {
      return StorageSetRequest.CONTRACT_ID;
   }

   public final int getSubContractId() {
      return StorageSetRequest.SUB_CONTRACT_ID;
   }

   public final int getStructId() {
      return StorageSetRequest.STRUCT_ID;
   }
   
   @Override
   public final Response dispatch(ServiceAPI is, RequestContext ctx) {
      if (is instanceof Handler)
         return ((Handler)is).requestStorageSet(this, ctx);
      return is.genericRequest(this, ctx);
   }
   
   public static interface Handler extends ServiceAPI {
      Response requestStorageSet(StorageSetRequest r, RequestContext ctx);
   }
   
   public final String[] tagWebNames() {
      // Note do not use this tags in long term serializations (to disk or databases) as 
      // implementors are free to rename them however they wish.  A null means the field
      // is not to participate in web serialization (remaining at default)
      String[] result = new String[2+1];
      result[1] = "key";
      result[2] = "value";
      return result;
   }
   
   public final Structure make() {
      return new StorageSetRequest();
   }
   
   public final StructDescription makeDescription() {
      StructDescription desc = new StructDescription();      
      desc.name = "StorageSetRequest";
      desc.tagWebNames = tagWebNames();
      desc.types = new TypeDescriptor[desc.tagWebNames.length];
      desc.types[0] = new TypeDescriptor(TypeDescriptor.T_STRUCT, getContractId(), getStructId());
      desc.types[1] = new TypeDescriptor(TypeDescriptor.T_STRING, 0, 0);
      desc.types[2] = new TypeDescriptor(TypeDescriptor.T_STRING, 0, 0);
      return desc;
   }

}
