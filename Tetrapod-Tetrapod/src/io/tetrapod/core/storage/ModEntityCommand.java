package io.tetrapod.core.storage;

import java.io.*;

import io.tetrapod.raft.Command;

public class ModEntityCommand implements Command<TetrapodStateMachine> {

   public static final int COMMAND_ID     = TetrapodStateMachine.MOD_ENTITY_COMMAND_ID;

   private final byte      commandVersion = 1;

   private int             entityId;
   private int             status;
   private String          build;
   private int             version;

   public ModEntityCommand() {}

   public ModEntityCommand(int entityId, int status, String build, int version) {
      this.entityId = entityId;
      this.status = status;
      this.build = build;
      this.version = version;
   }

   @Override
   public void applyTo(TetrapodStateMachine state) {
      state.updateEntity(entityId, status, build, version);
   }

   @Override
   public void write(DataOutputStream out) throws IOException {
      out.writeByte(commandVersion);
      out.writeInt(entityId);
      out.writeInt(status);
      out.writeUTF(build);
      out.writeInt(version);
   }

   @Override
   public void read(DataInputStream in, int fileVersion) throws IOException {
      byte commandVersion = in.readByte();
      assert commandVersion == this.commandVersion;
      entityId = in.readInt();
      status = in.readInt();
      build = in.readUTF();
      version = in.readInt();
   }

   @Override
   public int getCommandType() {
      return COMMAND_ID;
   }

   @Override
   public String toString() {
      return "ModEntityCommand(" + entityId + ", " + status + ", " + build + ", " + version + ")";
   }

   public static void register(TetrapodStateMachine state) {
      state.registerCommand(COMMAND_ID, () -> new ModEntityCommand());
   }

   public int getEntityId() {
      return entityId;
   }

}