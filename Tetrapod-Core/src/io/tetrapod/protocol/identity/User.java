package io.tetrapod.protocol.identity;

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
public class User extends Structure {
   
   public static final int PROPS_DEVELOPER = 1; 
   public static final int PROPS_ADMIN_T1 = 2; 
   public static final int PROPS_ADMIN_T2 = 4; 
   public static final int PROPS_ADMIN_T3 = 8; 
   public static final int PROPS_ADMIN_T4 = 16; 
   public static final int PROPS_BANNED_T1 = 32; 
   public static final int PROPS_BANNED_T2 = 64; 
   public static final int PROPS_BANNED_T3 = 128; 
   
   public static final int STRUCT_ID = 10894876;
   public static final int CONTRACT_ID = IdentityContract.CONTRACT_ID;
    
   public User() {
      defaults();
   }

   public User(String username, int accountId, int properties) {
      this.username = username;
      this.accountId = accountId;
      this.properties = properties;
   }   
   
   public String username;
   public int accountId;
   
   /**
    * bitmap
    */
   public int properties;

   public final Structure.Security getSecurity() {
      return Security.INTERNAL;
   }

   public final void defaults() {
      username = null;
      accountId = 0;
      properties = 0;
   }
   
   @Override
   public final void write(DataSource data) throws IOException {
      data.write(1, this.username);
      data.write(2, this.accountId);
      data.write(3, this.properties);
      data.writeEndTag();
   }
   
   @Override
   public final void read(DataSource data) throws IOException {
      defaults();
      while (true) {
         int tag = data.readTag();
         switch (tag) {
            case 1: this.username = data.read_string(tag); break;
            case 2: this.accountId = data.read_int(tag); break;
            case 3: this.properties = data.read_int(tag); break;
            case Codec.END_TAG:
               return;
            default:
               data.skip(tag);
               break;
         }
      }
   }
   
   public final int getContractId() {
      return User.CONTRACT_ID;
   }

   public final int getStructId() {
      return User.STRUCT_ID;
   }

   public final String[] tagWebNames() {
      // Note do not use this tags in long term serializations (to disk or databases) as 
      // implementors are free to rename them however they wish.  A null means the field
      // is not to participate in web serialization (remaining at default)
      String[] result = new String[3+1];
      result[1] = "username";
      result[2] = "accountId";
      result[3] = "properties";
      return result;
   }

   public final Structure make() {
      return new User();
   }

   public final StructDescription makeDescription() {
      StructDescription desc = new StructDescription();
      desc.tagWebNames = tagWebNames();
      desc.types = new TypeDescriptor[desc.tagWebNames.length];
      desc.types[0] = new TypeDescriptor(TypeDescriptor.T_STRUCT, getContractId(), getStructId());
      desc.types[1] = new TypeDescriptor(TypeDescriptor.T_STRING, 0, 0);
      desc.types[2] = new TypeDescriptor(TypeDescriptor.T_INT, 0, 0);
      desc.types[3] = new TypeDescriptor(TypeDescriptor.T_INT, 0, 0);
      return desc;
   }
}
