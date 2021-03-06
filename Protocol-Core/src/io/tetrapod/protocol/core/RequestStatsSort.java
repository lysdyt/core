package io.tetrapod.protocol.core;

// This is a code generated file.  All edits will be lost the next time code gen is run.

import io.*;
import io.tetrapod.core.rpc.*;
import java.util.*;

@SuppressWarnings("all")
public enum RequestStatsSort implements Enum_int<RequestStatsSort> {
   COUNT((int)1), 
   TOTAL_TIME((int)2), 
   AVERAGE_TIME((int)3), 
   ERRORS((int)4), 
   ;
   
   public static RequestStatsSort from(int val) {
      for (RequestStatsSort e : RequestStatsSort.values())
         if (e.value == (val))
            return e; 
      return null;
   }
   
   public final int value;
   
   /** 
    * Returns the value of the enum, as opposed to the name like the superclass.  To get
    * the name you can use this.name().
    * @return the value as a string
    */
   public String toString() { 
      return "" + value; 
   }
   
   private RequestStatsSort(int value) {
      this.value = value;
   }

   @Override
   public int getValue() {
      return value;
   }
}
