// automatically generated by the FlatBuffers compiler, do not modify

package com.riiablo.net.packet.mcp;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class CreateGame extends Table {
  public static CreateGame getRootAsCreateGame(ByteBuffer _bb) { return getRootAsCreateGame(_bb, new CreateGame()); }
  public static CreateGame getRootAsCreateGame(ByteBuffer _bb, CreateGame obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public CreateGame __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public int diff() { int o = __offset(4); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public int levelDifference() { int o = __offset(6); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public int maxPlayers() { int o = __offset(8); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public String gameName() { int o = __offset(10); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer gameNameAsByteBuffer() { return __vector_as_bytebuffer(10, 1); }
  public ByteBuffer gameNameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 10, 1); }
  public String password() { int o = __offset(12); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer passwordAsByteBuffer() { return __vector_as_bytebuffer(12, 1); }
  public ByteBuffer passwordInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 12, 1); }
  public String description() { int o = __offset(14); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer descriptionAsByteBuffer() { return __vector_as_bytebuffer(14, 1); }
  public ByteBuffer descriptionInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 14, 1); }
  public int gameToken() { int o = __offset(16); return o != 0 ? bb.getInt(o + bb_pos) : 0; }
  public int result() { int o = __offset(18); return o != 0 ? bb.getInt(o + bb_pos) : 0; }

  public static int createCreateGame(FlatBufferBuilder builder,
      int diff,
      int levelDifference,
      int maxPlayers,
      int gameNameOffset,
      int passwordOffset,
      int descriptionOffset,
      int gameToken,
      int result) {
    builder.startObject(8);
    CreateGame.addResult(builder, result);
    CreateGame.addGameToken(builder, gameToken);
    CreateGame.addDescription(builder, descriptionOffset);
    CreateGame.addPassword(builder, passwordOffset);
    CreateGame.addGameName(builder, gameNameOffset);
    CreateGame.addMaxPlayers(builder, maxPlayers);
    CreateGame.addLevelDifference(builder, levelDifference);
    CreateGame.addDiff(builder, diff);
    return CreateGame.endCreateGame(builder);
  }

  public static void startCreateGame(FlatBufferBuilder builder) { builder.startObject(8); }
  public static void addDiff(FlatBufferBuilder builder, int diff) { builder.addInt(0, diff, 0); }
  public static void addLevelDifference(FlatBufferBuilder builder, int levelDifference) { builder.addInt(1, levelDifference, 0); }
  public static void addMaxPlayers(FlatBufferBuilder builder, int maxPlayers) { builder.addInt(2, maxPlayers, 0); }
  public static void addGameName(FlatBufferBuilder builder, int gameNameOffset) { builder.addOffset(3, gameNameOffset, 0); }
  public static void addPassword(FlatBufferBuilder builder, int passwordOffset) { builder.addOffset(4, passwordOffset, 0); }
  public static void addDescription(FlatBufferBuilder builder, int descriptionOffset) { builder.addOffset(5, descriptionOffset, 0); }
  public static void addGameToken(FlatBufferBuilder builder, int gameToken) { builder.addInt(6, gameToken, 0); }
  public static void addResult(FlatBufferBuilder builder, int result) { builder.addInt(7, result, 0); }
  public static int endCreateGame(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

