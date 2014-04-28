package io.tetrapod.core;

import io.tetrapod.protocol.core.*;

import org.slf4j.*;

public class TestService extends DefaultService  {
   public static final Logger logger = LoggerFactory.getLogger(TestService.class);

   public TestService() {
      setMainContract(new TetrapodContract());
      addPeerContracts(/* non-core services we talk to: eg:*//* new WalletContract(), new StorageContract() */);
   }

   @Override
   public void onRegistered() {

      updateStatus(status & ~Core.STATUS_STARTING);
   }

   @Override
   public void onShutdown(boolean restarting) {}

   @Override
   public String getServiceIcon() {
      return "media/identity.png";
   }
}