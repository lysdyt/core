package io.tetrapod.core.rpc;

import io.tetrapod.core.serialize.DataSource;
import io.tetrapod.protocol.core.StructDescription;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

abstract public class Structure {

   public static enum Security {
      PUBLIC, // open to services and unauthorized users
      PROTECTED, // open to services and authorized users
      INTERNAL, // open to services
      PRIVATE, // open to exact same service only
      ADMIN // open to admin user only
   }

   abstract public void write(DataSource data) throws IOException;

   abstract public void read(DataSource data) throws IOException;

   abstract public int getStructId();

   abstract public int getContractId();

   public Structure make() {
      try {
         return getClass().newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
         return null;
      }
   }

   public StructDescription makeDescription() {
      return null;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName();
   }

   public String dump() {
      return dump(true, true);
   }

   public String unsafeDump() {
      return unsafeDump(true, true);
   }

   public String dump(boolean expandSubTypes, boolean includeClassname) {
      StringBuilder sb = new StringBuilder();
      Field f[] = getClass().getDeclaredFields();

      for (int i = 0; i < f.length; i++) {
         try {
            int mod = f[i].getModifiers();
            if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
               String name = f[i].getName();
               Object val = isSensitive(name) ? "~" : dumpValue(f[i].get(this), expandSubTypes);
               sb.append(f[i].getName() + ":" + val + ", ");
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      String s = sb.length() > 0 ? ("{ " + sb.substring(0, sb.length() - 2) + " } ") : "{}";
      return (includeClassname ? this.getClass().getSimpleName() + " " : "") + s;
   }
   
   public String unsafeDump(boolean expandSubTypes, boolean includeClassname) {
      StringBuilder sb = new StringBuilder();
      Field f[] = getClass().getDeclaredFields();

      for (int i = 0; i < f.length; i++) {
         try {
            int mod = f[i].getModifiers();
            if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
               Object val = dumpValue(f[i].get(this), expandSubTypes);
               sb.append(f[i].getName() + ":" + val + ", ");
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      String s = sb.length() > 0 ? ("{ " + sb.substring(0, sb.length() - 2) + " } ") : "{}";
      return (includeClassname ? this.getClass().getSimpleName() + " " : "") + s;
   }


   public Security getSecurity() {
      return Security.INTERNAL;
   }

   public String[] tagWebNames() {
      return new String[] {};
   }

   @SuppressWarnings("rawtypes")
   protected Object dumpValue(Object val, boolean expandSubTypes) {
      if (val != null && val instanceof List) {
         List list = ((List) val);
         if (list.size() <= 3) {
            val = list.toString();
         } else {
            val = "[len=" + list.size() + "]";
         }
      }
      if (val != null && val.getClass().isArray()) {
         int n = Array.getLength(val);
         switch (n) {
            case 0: return "[]";
            case 1: return "[" + Array.get(val, 0).toString() + "]";
            case 2: return "[" + Array.get(val, 0).toString() + "," + Array.get(val, 1).toString() + "]";
            case 3: return "[" + Array.get(val, 0).toString() + "," + Array.get(val, 1).toString() + "," + Array.get(val, 2).toString() + "]";
            default:
               return "[len=" + n + "]";
         }
      }
      if (expandSubTypes && val != null && val instanceof Structure) {
         val = ((Structure) val).dump(false, false);
      }
      if (expandSubTypes && val instanceof String) {         
         if (((String) val).length() > 2048) {
            return ((String) val).substring(0, 2048) + "...";
         }
      }
      return val;
   }
   
   public Object toRawForm(DataSource ds) {
      try {
         write(ds);
         return ds.getUnderlyingObject();
      } catch (IOException e) {
         return null;
      }
   }
   
   public boolean isSensitive(String fieldName) {
      return false;
   }

}
