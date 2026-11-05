package net.momirealms.craftengine.bedrock.network;

import java.nio.ByteBuffer;

public final class BedrockProtocol {
    public static final int VERSION = 2;
    public static final String HANDSHAKE_CHANNEL = "craftengine:bedrock_handshake";

    private BedrockProtocol() {
    }

    public static byte[] handshake(int protocolVersion) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(protocolVersion).array();
    }

    public static int readHandshake(ByteBuffer data) {
        return data.remaining() == Integer.BYTES ? data.getInt() : -1;
    }
}
