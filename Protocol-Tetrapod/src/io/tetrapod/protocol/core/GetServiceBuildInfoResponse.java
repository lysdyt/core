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

@SuppressWarnings("all")
public class GetServiceBuildInfoResponse extends Response {
   
   public static final int STRUCT_ID = 4037623;
   public static final int CONTRACT_ID = TetrapodContract.CONTRACT_ID;
   public static final int SUB_CONTRACT_ID = TetrapodContract.SUB_CONTRACT_ID;

   public GetServiceBuildInfoResponse() {
      defaults();
   }

   public GetServiceBuildInfoResponse(List<BuildInfo> services) {
      this.services = services;
   }   
   
   public List<BuildInfo> services;

   public final Structure.Security getSecurity() {
      return Security.ADMIN;
   }

   public final void defaults() {
      services = null;
   }
   
   @Override
   public final void write(DataSource data) throws IOException {
      if (this.services != null) data.write_struct(1, this.services);
      data.writeEndTag();
   }
   
   @Override
   public final void read(DataSource data) throws IOException {
      defaults();
      while (true) {
         int tag = data.readTag();
         switch (tag) {
            case 1: this.services = data.read_struct_list(tag, new BuildInfo()); break;
            case Codec.END_TAG:
               return;
            default:
               data.skip(tag);
               break;
         }
      }
   }
  
   public final int getContractId() {
      return GetServiceBuildInfoResponse.CONTRACT_ID;
   }

   public final int getSubContractId() {
      return GetServiceBuildInfoResponse.SUB_CONTRACT_ID;
   }

   public final int getStructId() {
      return GetServiceBuildInfoResponse.STRUCT_ID;
   }

   public final String[] tagWebNames() {
      // Note do not use this tags in long term serializations (to disk or databases) as 
      // implementors are free to rename them however they wish.  A null means the field
      // is not to participate in web serialization (remaining at default)
      String[] result = new String[1+1];
      result[1] = "services";
      return result;
   }

   public final Structure make() {
      return new GetServiceBuildInfoResponse();
   }

   public final StructDescription makeDescription() {
      StructDescription desc = new StructDescription();      
      desc.name = "GetServiceBuildInfoResponse";
      desc.tagWebNames = tagWebNames();
      desc.types = new TypeDescriptor[desc.tagWebNames.length];
      desc.types[0] = new TypeDescriptor(TypeDescriptor.T_STRUCT, getContractId(), getStructId());
      desc.types[1] = new TypeDescriptor(TypeDescriptor.T_STRUCT_LIST, BuildInfo.CONTRACT_ID, BuildInfo.STRUCT_ID);
      return desc;
   }
 }
