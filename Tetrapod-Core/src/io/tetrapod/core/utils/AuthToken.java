package io.tetrapod.core.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.netty.buffer.*;
import io.netty.handler.codec.base64.*;
import io.tetrapod.core.serialize.datasources.ByteBufDataSource;

/**
 * Helper class used to authenticate messages. Works by taking a message (an array of
 * ints) and computing an HMAC using a shared secret. The token is then a subset(input
 * vals) + HMAC. Upon decoding the subset of input vals are recovered, combined with any
 * values known through other means, and the HMAC is recomputed and checked for validity.
 * <p>
 * Note that the actual data encoded and decoded in the token is up to the caller.
 */
public class AuthToken {

   private static final long NOT_THAT_LONG_AGO = 1395443029600L;

   /**
    * Creates a MAC from a secret and some additional
    */
   public static Mac createMac(byte[] secret1, byte[] secret2) {
      try {
         // append NTLA so updating it will invalidate old tokens
         byte[] ntla = Long.toHexString(NOT_THAT_LONG_AGO).getBytes();
         byte[] key = new byte[secret1.length + ntla.length + secret2.length];
         System.arraycopy(ntla, 0, key, 0, ntla.length);
         System.arraycopy(secret1, 0, key, ntla.length, secret1.length);
         System.arraycopy(secret2, 0, key, ntla.length + secret1.length, secret2.length);
         SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
         Mac macCoder = Mac.getInstance("HmacSHA1");
         macCoder.init(signingKey);
         return macCoder;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   /**
    * Creates a MAC from a secret and some additional
    */
   public static Mac createMacLegacy(byte[] secret) {
      try {
         // append NTLA so updating it will invalidate old tokens
         byte[] ntla = Long.toHexString(NOT_THAT_LONG_AGO).getBytes();
         byte[] key = new byte[secret.length + ntla.length];
         System.arraycopy(secret, 0, key, 0, secret.length);
         System.arraycopy(ntla, 0, key, secret.length, ntla.length);
         SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
         Mac macCoder = Mac.getInstance("HmacSHA1");
         macCoder.init(signingKey);
         return macCoder;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   /**
    * Sets the shared secret. Needs to be called before this class is used. Returns false
    * if there is an error which would typically be Java not having strong crypto
    * available.
    */
   public static boolean setSecret(byte[] secret) {
      try {
         LoginAuthToken.setSecret(secret);
         LegacyAuthToken.setSecret(secret);
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   /**
    * Encodes a auth token with all passed in values also present in the auth token.
    * 
    * @param values the values which form the basis of the token
    * @return the base64 encoded token
    */
   public static String encode(Mac theMac, int... values) {
      return encode(theMac, values, values.length);
   }

   /**
    * Encodes a auth token. The numInToken parameter is a bandwidth micro-optimization.
    * Say for example we wanted to make an auth token with one value being the entityId.
    * We *could* encode the true value for the entity id in the token, but since it is
    * already present in the header we could leave it out of the token and supply it at
    * decode time.
    * 
    * @param values the values which form the basis of the token
    * @param numInToken the number of values which will need to be encoded inside the
    *           token
    * @return the base64 encoded token
    */
   protected static String encode(Mac theMAC, int[] values, int numInToken) {
      ByteBuf buf = Unpooled.buffer();
      try {
         ByteBufDataSource bds = new ByteBufDataSource(buf);
         for (int i = 0; i < values.length; i++) {
            bds.writeVarInt(values[i]);
         }
         byte[] mac;
         synchronized (AuthToken.class) {
            theMAC.update(buf.array(), buf.arrayOffset(), buf.writerIndex());
            mac = theMAC.doFinal();
         }
         buf.resetWriterIndex();
         for (int i = 0; i < numInToken; i++) {
            bds.writeVarInt(values[i]);
         }
         ByteBuf encodedBuf = Base64.encode(buf, Base64Dialect.URL_SAFE);
         try {
            ByteBuf macBuf = Unpooled.wrappedBuffer(mac);
            try {
               ByteBuf encodedMacBuf = Base64.encode(macBuf, Base64Dialect.URL_SAFE);
               try {
                  return encodedBuf.toString(Charset.forName("UTF-8")) + encodedMacBuf.toString(Charset.forName("UTF-8"));
               } finally {
                  encodedMacBuf.release();
               }
            } finally {
               macBuf.release();
            }
         } finally {
            encodedBuf.release();
         }
      } catch (IOException e) {

      } finally {
         buf.release();
      }
      return null;
   }

   /**
    * Decodes an auth token. If there is at least one value in the token it assumes the
    * first value is a timeout value and checks it versus the current time.
    * 
    * @param values the values of the token, the first numInToken elements get filled in
    *           from the token
    * @param numInToken the number of values to pull out from the token
    * @param token the base64 encoded token
    * @return true if it decodes successfully, and as a side effect fills in values with
    *         any values which were encoded in token
    */
   public static boolean decode(Mac theMac, int[] values, int numInToken, String token) {
      ByteBuf tokenBuf = null;
      try {
         tokenBuf = Base64.decode(Unpooled.wrappedBuffer(token.getBytes()), Base64Dialect.URL_SAFE);

         ByteBufDataSource bds = new ByteBufDataSource(tokenBuf);
         for (int i = 0; i < numInToken; i++) {
            values[i] = bds.readVarInt();
         }
         String encoded = encode(theMac, values, numInToken);
         return encoded.equals(token);
      } catch (Exception e) {} finally {
         if (tokenBuf != null) {
            tokenBuf.release();
         }
      }
      return false;
   }

   /**
    * Return the number of minutes since NOT_THAT_LONG_AGO
    */
   public static int timeNowInMinutes() {
      long delta = System.currentTimeMillis() - NOT_THAT_LONG_AGO;
      return (int) (delta / 60000);
   }

   public static class Decoded {
      public int   accountId;
      public int   timeLeft;
      public int[] miscValues;
   }

   public static String generateSharedSecret() {
      return Base64.encode(Unpooled.wrappedBuffer(generateRandomBytes(64)), Base64Dialect.STANDARD).toString(Charset.forName("UTF-8"));
   }

   public static byte[] generateRandomBytes(int len) {
      byte[] b = new byte[len];
      Random r = new SecureRandom();
      r.nextBytes(b);
      return b;
   }
}
