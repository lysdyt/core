package io.tetrapod.core.storage;

import io.tetrapod.raft.*;
import io.tetrapod.raft.StateMachine.CommandFactory;

import java.io.*;

public class ReleaseOwnershipCommand implements Command<TetrapodStateMachine> {

   public static final int COMMAND_ID = TetrapodStateMachine.RELEASE_OWNERSHIP_COMMAND_ID;

   private int             ownerId;
   private String[]        keys;

   public ReleaseOwnershipCommand() {}

   public ReleaseOwnershipCommand(int ownerId, String[] keys) {
      this.ownerId = ownerId;
      this.keys = keys;
   }

   @Override
   public void applyTo(TetrapodStateMachine state) {
      state.releaseOwnership(ownerId, keys);
   }

   @Override
   public void write(DataOutputStream out) throws IOException {
      out.writeInt(ownerId);
      if (keys == null) {
         out.writeInt(0);
      } else {
         out.writeInt(keys.length);
         for (String key : keys) {
            out.writeUTF(key);
         }
      }
   }

   @Override
   public void read(DataInputStream in, int fileVersion) throws IOException {
      ownerId = in.readInt();
      final int numKeys = in.readInt();
      if (numKeys > 0) {
         keys = new String[numKeys];
         for (int i = 0; i < numKeys; i++) {
            keys[i] = in.readUTF();
         }
      }
   }

   @Override
   public int getCommandType() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "ReleaseOwnershipCommand(" + ownerId + ", " + keys + ")";
   }

   public static void register(TetrapodStateMachine state) {
      state.registerCommand(COMMAND_ID, new CommandFactory<TetrapodStateMachine>() {
         @Override
         public Command<TetrapodStateMachine> makeCommand() {
            return new ReleaseOwnershipCommand();
         }
      });
   }

   public int getOwnerId() {
      return ownerId;
   }

   public String[] getKeys() {
      return keys;
   }

}
