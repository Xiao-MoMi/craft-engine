package net.momirealms.craftengine.core.plugin.network;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

/** Movement payloads after the entity id in the 26.3 protocol. */
public final class EntityMovement26_3 {
    private EntityMovement26_3() {}

    public static void readDelta(FriendlyByteBuf buf, DeltaConsumer consumer) {
        int steps = buf.readVarInt() >>> 1;
        if (steps == 0) {
            consumer.accept(buf.readShort(), buf.readShort(), buf.readShort());
        } else {
            for (int i = 0; i < steps; i++) {
                buf.readVarInt(); // tick offset
                consumer.accept(buf.readShort(), buf.readShort(), buf.readShort());
            }
        }
    }

    public static PacketPosition readPosition(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        if (type == 1) {
            int steps = buf.readVarInt();
            PacketPosition position = null;
            for (int i = 0; i < steps; i++) {
                position = new PacketPosition(buf.readDouble(), buf.readDouble(), buf.readDouble());
                buf.readVarInt(); // tick offset
            }
            return position;
        }
        return new PacketPosition(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    @FunctionalInterface
    public interface DeltaConsumer {
        void accept(short x, short y, short z);
    }
}
