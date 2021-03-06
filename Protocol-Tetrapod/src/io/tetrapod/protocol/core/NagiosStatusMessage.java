package io.tetrapod.protocol.core;

// This is a code generated file.  All edits will be lost the next time code gen is run.

import io.*;
import io.tetrapod.core.serialize.*;
import io.tetrapod.core.rpc.*;
import io.tetrapod.protocol.core.TypeDescriptor;
import io.tetrapod.protocol.core.StructDescription;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings("all")
public class NagiosStatusMessage extends Message {
   
   public static final int STRUCT_ID = 6683577;
   public static final int CONTRACT_ID = TetrapodContract.CONTRACT_ID;
   public static final int SUB_CONTRACT_ID = TetrapodContract.SUB_CONTRACT_ID;

   public NagiosStatusMessage() {
      defaults();
   }

   public NagiosStatusMessage(String hostname, boolean enabled) {
      this.hostname = hostname;
      this.enabled = enabled;
   }   
   
   public String hostname;
   public boolean enabled;

   public final Structure.Security getSecurity() {
      return Security.INTERNAL;
   }

   public final void defaults() {
      hostname = null;
      enabled = false;
   }
   
   @Override
   public final void write(DataSource data) throws IOException {
      data.write(1, this.hostname);
      data.write(2, this.enabled);
      data.writeEndTag();
   }
   
   @Override
   public final void read(DataSource data) throws IOException {
      defaults();
      while (true) {
         int tag = data.readTag();
         switch (tag) {
            case 1: this.hostname = data.read_string(tag); break;
            case 2: this.enabled = data.read_boolean(tag); break;
            case Codec.END_TAG:
               return;
            default:
               data.skip(tag);
               break;
         }
      }
   }
   
   public final int getContractId() {
      return NagiosStatusMessage.CONTRACT_ID;
   }

   public final int getSubContractId() {
      return NagiosStatusMessage.SUB_CONTRACT_ID;
   }

   public final int getStructId() {
      return NagiosStatusMessage.STRUCT_ID;
   }
   
   @Override
   public final void dispatch(SubscriptionAPI api, MessageContext ctx) {
      if (api instanceof Handler)
         ((Handler)api).messageNagiosStatus(this, ctx);
      else
         api.genericMessage(this, ctx);
   }
   
   public static interface Handler extends SubscriptionAPI {
      void messageNagiosStatus(NagiosStatusMessage m, MessageContext ctx);
   }
   
   public final String[] tagWebNames() {
      // Note do not use this tags in long term serializations (to disk or databases) as 
      // implementors are free to rename them however they wish.  A null means the field
      // is not to participate in web serialization (remaining at default)
      String[] result = new String[2+1];
      result[1] = "hostname";
      result[2] = "enabled";
      return result;
   }
   
   public final Structure make() {
      return new NagiosStatusMessage();
   }
   
   public final StructDescription makeDescription() {
      StructDescription desc = new StructDescription();     
      desc.name = "NagiosStatusMessage";
      desc.tagWebNames = tagWebNames();
      desc.types = new TypeDescriptor[desc.tagWebNames.length];
      desc.types[0] = new TypeDescriptor(TypeDescriptor.T_STRUCT, getContractId(), getStructId());
      desc.types[1] = new TypeDescriptor(TypeDescriptor.T_STRING, 0, 0);
      desc.types[2] = new TypeDescriptor(TypeDescriptor.T_BOOLEAN, 0, 0);
      return desc;
   }
}
