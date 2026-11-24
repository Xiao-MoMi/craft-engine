package net.momirealms.craftengine.bedrock.network;

import java.nio.ByteBuffer;

public final class BedrockProtocol {
    public static final int VERSION = 7;
    public static final String HANDSHAKE_CHANNEL = "craftengine:bedrock_handshake";
    public static final String CREATIVE_ITEMS_CHANNEL = "craftengine:bedrock_creative_items";
    public static final int CREATIVE_ITEMS_CHUNK_BYTES = 512 * 1024;

    private BedrockProtocol() {
    }

    public static byte[] handshake(int protocolVersion) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(protocolVersion).array();
    }

    public static byte[] handshake(int protocolVersion, int javaProtocol) {
        return ByteBuffer.allocate(2 * Integer.BYTES).putInt(protocolVersion).putInt(javaProtocol).array();
    }

    public static int readHandshake(ByteBuffer data) {
        return data.remaining() == Integer.BYTES ? data.getInt() : -1;
    }
}
