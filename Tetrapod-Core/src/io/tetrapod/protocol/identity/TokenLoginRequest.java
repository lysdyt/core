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
public class TokenLoginRequest extends Request {

   public static final int STRUCT_ID = 11214546;
   public static final int CONTRACT_ID = IdentityContract.CONTRACT_ID;
   
   public TokenLoginRequest() {
      defaults();
   }

   public TokenLoginRequest(String authToken, int tokenType, int accountId) {
      this.authToken = authToken;
      this.tokenType = tokenType;
      this.accountId = accountId;
   }   

   public String authToken;
   public int tokenType;
   
   /**
    * only required for AUTH_TOKEN_USER logins
    */
   public int accountId;

   public final Structure.Security getSecurity() {
      return Security.PUBLIC;
   }

   public final void defaults() {
      authToken = null;
      tokenType = 0;
      accountId = 0;
   }
   
   @Override
   public final void write(DataSource data) throws IOException {
      data.write(1, this.authToken);
      data.write(2, this.tokenType);
      data.write(3, this.accountId);
      data.writeEndTag();
   }
   
   @Override
   public final void read(DataSource data) throws IOException {
      defaults();
      while (true) {
         int tag = data.readTag();
         switch (tag) {
            case 1: this.authToken = data.read_string(tag); break;
            case 2: this.tokenType = data.read_int(tag); break;
            case 3: this.accountId = data.read_int(tag); break;
            case Codec.END_TAG:
               return;
            default:
               data.skip(tag);
               break;
         }
      }
   }
   
   public final int getContractId() {
      return TokenLoginRequest.CONTRACT_ID;
   }

   public final int getStructId() {
      return TokenLoginRequest.STRUCT_ID;
   }
   
   @Override
   public final Response dispatch(ServiceAPI is, RequestContext ctx) {
      if (is instanceof Handler)
         return ((Handler)is).requestTokenLogin(this, ctx);
      return is.genericRequest(this, ctx);
   }
   
   public static interface Handler extends ServiceAPI {
      Response requestTokenLogin(TokenLoginRequest r, RequestContext ctx);
   }
   
   public final String[] tagWebNames() {
      // Note do not use this tags in long term serializations (to disk or databases) as 
      // implementors are free to rename them however they wish.  A null means the field
      // is not to participate in web serialization (remaining at default)
      String[] result = new String[3+1];
      result[1] = "authToken";
      result[2] = "tokenType";
      result[3] = "accountId";
      return result;
   }
   
   public final Structure make() {
      return new TokenLoginRequest();
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