package net.momirealms.craftengine.bedrock.geyser.network;

import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class GeyserNetworkManager {
    private final Map<Class<?>, PacketListeners<?>> incoming = new IdentityHashMap<>();
    private final Map<Class<?>, PacketListeners<?>> outgoing = new IdentityHashMap<>();

    public <T extends BedrockPacket> void onReceive(Class<T> type, Consumer<GeyserPacketEvent<T>> listener) {
        this.listeners(this.incoming, type).listeners.add(listener);
    }

    public <T extends BedrockPacket> void onSend(Class<T> type, Consumer<GeyserPacketEvent<T>> listener) {
        this.listeners(this.outgoing, type).listeners.addFirst(listener);
    }

    @SuppressWarnings("unchecked")
    private <T extends BedrockPacket> PacketListeners<T> listeners(Map<Class<?>, PacketListeners<?>> registry, Class<T> type) {
        return (PacketListeners<T>) registry.computeIfAbsent(type, ignored -> new PacketListeners<>(type));
    }

    public boolean receive(BedrockPacketWrapper wrapper) {
        PacketListeners<?> listeners = this.incoming.get(wrapper.getPacket().getClass());
        return listeners != null && listeners.dispatch(wrapper);
    }

    public boolean send(BedrockPacketWrapper wrapper) {
        PacketListeners<?> listeners = this.outgoing.get(wrapper.getPacket().getClass());
        return listeners != null && listeners.dispatch(wrapper);
    }

    private static final class PacketListeners<T extends BedrockPacket> {
        private final Class<T> type;
        private final List<Consumer<GeyserPacketEvent<T>>> listeners = new ArrayList<>();

        private PacketListeners(Class<T> type) {
            this.type = type;
        }

        private boolean dispatch(BedrockPacketWrapper wrapper) {
            GeyserPacketEvent<T> event = new GeyserPacketEvent<>(wrapper, this.type);
            for (int i = 0; i < this.listeners.size(); i++) {
                this.listeners.get(i).accept(event);
                if (event.isCancelled()) {
                    return true;
                }
            }
            return false;
        }
    }
}
