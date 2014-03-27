package io.tetrapod.core.web;

import io.tetrapod.protocol.core.WebRoute;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebRoutes {

   private Map<String,WebRoute> routes = new ConcurrentHashMap<>();
   
   public void setRoute(String path, int contractId, int structId) {
      // "/api/LoginRequest  contractId=2, structId=1244334
      WebRoute r = new WebRoute(path, structId, contractId);
      routes.put(path, r);
   }
   
   public WebRoute findRoute(String uri) {
      int ix = uri.indexOf('?');
      String path = ix < 0 ? uri : uri.substring(0, ix);
      return routes.get(path);
   }
   

}