package io.tetrapod.protocol.core;

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
public class SnapshotRequest extends Request {

   public static final int STRUCT_ID = 7083092;
   public static final int CONTRACT_ID = TetrapodContract.CONTRACT_ID;
   public static final int SUB_CONTRACT_ID = TetrapodContract.SUB_CONTRACT_ID;

   public SnapshotRequest() {
      defaults();
   }

   public SnapshotRequest(int accountId, String authToken) {
      this.accountId = accountId;
      this.authToken = authToken;
   }   

   public int accountId;
   public String authToken;

   public final Structure.Security getSecurity() {
      return Security.ADMIN;
   }

   public final void defaults() {
      accountId = 0;
      authToken = null;
   }
   
   @Override
   public final void write(DataSource data) throws IOException {
      data.write(1, this.accountId);
      data.write(2, this.authToken);
      data.writeEndTag();
   }
   
   @Override
   public final void read(DataSource data) throws IOException {
      defaults();
      while (true) {
         int tag = data.readTag();
         switch (tag) {
            case 1: this.accountId = data.read_int(tag); break;
            case 2: this.authToken = data.read_string(tag); break;
            case Codec.END_TAG:
               return;
            default:
               data.skip(tag);
               break;
         }
      }
   }
   
   public final int getContractId() {
      return SnapshotRequest.CONTRACT_ID;
   }

   public final int getSubContractId() {
      return SnapshotRequest.SUB_CONTRACT_ID;
   }

   public final int getStructId() {
      return SnapshotRequest.STRUCT_ID;
   }
   
   @Override
   public final Response dispatch(ServiceAPI is, RequestContext ctx) {
      if (is instanceof Handler)
         return ((Handler)is).requestSnapshot(this, ctx);
      return is.genericRequest(this, ctx);
   }
   
   public static interface Handler extends ServiceAPI {
      Response requestSnapshot(SnapshotRequest r, RequestContext ctx);
   }
   
   public final String[] tagWebNames() {
      // Note do not use this tags in long term serializations (to disk or databases) as 
      // implementors are free to rename them however they wish.  A null means the field
      // is not to participate in web serialization (remaining at default)
      String[] result = new String[2+1];
      result[1] = "accountId";
      result[2] = "authToken";
      return result;
   }
   
   public final Structure make() {
      return new SnapshotRequest();
   }
   
   public final StructDescription makeDescription() {
      StructDescription desc = new StructDescription();      
      desc.name = "SnapshotRequest";
      desc.tagWebNames = tagWebNames();
      desc.types = new TypeDescriptor[desc.tagWebNames.length];
      desc.types[0] = new TypeDescriptor(TypeDescriptor.T_STRUCT, getContractId(), getStructId());
      desc.types[1] = new TypeDescriptor(TypeDescriptor.T_INT, 0, 0);
      desc.types[2] = new TypeDescriptor(TypeDescriptor.T_STRING, 0, 0);
      return desc;
   }

   public final Response securityCheck(RequestContext ctx) {
      return ctx.securityCheck(this, accountId, authToken, Admin.RIGHTS_CLUSTER_WRITE);
   }
       
   @Override
   public boolean isSensitive(String fieldName) {
      if (fieldName.equals("authToken")) return true;
      return false;
   }
}
