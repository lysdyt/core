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
public class StructDescription extends Structure {
   
   public static final int STRUCT_ID = 9642196;
   public static final int CONTRACT_ID = TetrapodContract.CONTRACT_ID;
    
   public StructDescription() {
      defaults();
   }

   public StructDescription(TypeDescriptor[] types, String[] tagWebNames) {
      this.types = types;
      this.tagWebNames = tagWebNames;
   }   
   
   public TypeDescriptor[] types;
   public String[] tagWebNames;

   public final Structure.Security getSecurity() {
      return Security.INTERNAL;
   }

   public final void defaults() {
      types = null;
      tagWebNames = null;
   }
   
   @Override
   public final void write(DataSource data) throws IOException {
      if (this.types != null) data.write(1, this.types);
      if (this.tagWebNames != null) data.write(2, this.tagWebNames);
      data.writeEndTag();
   }
   
   @Override
   public final void read(DataSource data) throws IOException {
      defaults();
      while (true) {
         int tag = data.readTag();
         switch (tag) {
            case 1: this.types = data.read_struct_array(tag, new TypeDescriptor()); break;
            case 2: this.tagWebNames = data.read_string_array(tag); break;
            case Codec.END_TAG:
               return;
            default:
               data.skip(tag);
               break;
         }
      }
   }
   
   public final int getContractId() {
      return StructDescription.CONTRACT_ID;
   }

   public final int getStructId() {
      return StructDescription.STRUCT_ID;
   }

   public final String[] tagWebNames() {
      // Note do not use this tags in long term serializations (to disk or databases) as 
      // implementors are free to rename them however they wish.  A null means the field
      // is not to participate in web serialization (remaining at default)
      String[] result = new String[2+1];
      result[1] = "types";
      result[2] = "tagWebNames";
      return result;
   }

   public final Structure make() {
      return new StructDescription();
   }

   public final StructDescription makeDescription() {
      StructDescription desc = new StructDescription();
      desc.tagWebNames = tagWebNames();
      desc.types = new TypeDescriptor[desc.tagWebNames.length];
      desc.types[0] = new TypeDescriptor(TypeDescriptor.T_STRUCT, getContractId(), getStructId());
      desc.types[1] = new TypeDescriptor(TypeDescriptor.T_STRUCT_LIST, TypeDescriptor.CONTRACT_ID, TypeDescriptor.STRUCT_ID);
      desc.types[2] = new TypeDescriptor(TypeDescriptor.T_STRING_LIST, 0, 0);
      return desc;
   }
}
