package net.momirealms.craftengine.bedrock.geyser.network;

import io.netty.util.ReferenceCountUtil;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

public final class GeyserPacketEvent<T extends BedrockPacket> {
    private final BedrockPacketWrapper wrapper;
    private T packet;
    private boolean cancelled;

    GeyserPacketEvent(BedrockPacketWrapper wrapper, Class<T> type) {
        this.wrapper = wrapper;
        this.packet = type.cast(wrapper.getPacket());
    }

    public T packet() {
        return this.packet;
    }

    public void setPacket(T packet) {
        BedrockPacket previous = this.wrapper.getPacket();
        if (previous == packet) {
            return;
        }
        // The wrapper takes ownership of the replacement; cached bytes belong to the old packet.
        this.packet = packet;
        this.wrapper.setPacket(packet);
        ReferenceCountUtil.release(previous);
        ReferenceCountUtil.release(this.wrapper.getPacketBuffer());
        this.wrapper.setPacketBuffer(null);
        this.wrapper.setHeaderLength((byte) 0);
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
