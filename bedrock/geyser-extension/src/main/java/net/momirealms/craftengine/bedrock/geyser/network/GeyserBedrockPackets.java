package net.momirealms.craftengine.bedrock.geyser.network;

import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class GeyserBedrockPackets {
    private final ListenerTable incoming = new ListenerTable(ListenerOrder.OLDEST_FIRST);
    private final ListenerTable outgoing = new ListenerTable(ListenerOrder.NEWEST_FIRST);

    public <T extends BedrockPacket> void onReceive(Class<T> type, Consumer<GeyserPacketEvent<T>> listener) {
        this.incoming.register(type, listener);
    }

    public <T extends BedrockPacket> void onSend(Class<T> type, Consumer<GeyserPacketEvent<T>> listener) {
        this.outgoing.register(type, listener);
    }

    public boolean receive(BedrockPacketWrapper wrapper) {
        return this.incoming.dispatch(wrapper);
    }

    public boolean send(BedrockPacketWrapper wrapper) {
        return this.outgoing.dispatch(wrapper);
    }

    private enum ListenerOrder {
        OLDEST_FIRST,
        NEWEST_FIRST
    }

    private static final class ListenerTable {
        private final Map<Class<? extends BedrockPacket>, PacketListeners<?>> byType = new IdentityHashMap<>();
        private final ListenerOrder order;

        private ListenerTable(ListenerOrder order) {
            this.order = order;
        }

        @SuppressWarnings("unchecked")
        private <T extends BedrockPacket> void register(Class<T> type, Consumer<GeyserPacketEvent<T>> listener) {
            PacketListeners<T> listeners = (PacketListeners<T>) this.byType.computeIfAbsent(type, ignored -> new PacketListeners<>(type));
            listeners.add(listener, this.order);
        }

        private boolean dispatch(BedrockPacketWrapper wrapper) {
            PacketListeners<?> listeners = this.byType.get(wrapper.getPacket().getClass());
            return listeners != null && listeners.dispatch(wrapper);
        }
    }

    private static final class PacketListeners<T extends BedrockPacket> {
        private static final Consumer<?>[] EMPTY_LISTENERS = new Consumer<?>[0];
        private final Class<T> type;
        @SuppressWarnings("unchecked")
        private Consumer<GeyserPacketEvent<T>>[] listeners = (Consumer<GeyserPacketEvent<T>>[]) EMPTY_LISTENERS;

        private PacketListeners(Class<T> type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        private void add(Consumer<GeyserPacketEvent<T>> listener, ListenerOrder order) {
            Consumer<GeyserPacketEvent<T>>[] previous = this.listeners;
            Consumer<GeyserPacketEvent<T>>[] updated = (Consumer<GeyserPacketEvent<T>>[]) new Consumer<?>[previous.length + 1];
            if (order == ListenerOrder.OLDEST_FIRST) {
                System.arraycopy(previous, 0, updated, 0, previous.length);
                updated[previous.length] = listener;
            } else {
                System.arraycopy(previous, 0, updated, 1, previous.length);
                updated[0] = listener;
            }
            this.listeners = updated;
        }

        private boolean dispatch(BedrockPacketWrapper wrapper) {
            GeyserPacketEvent<T> event = new GeyserPacketEvent<>(wrapper, this.type);
            for (int i = 0; i < this.listeners.length; i++) {
                this.listeners[i].accept(event);
                if (event.isCancelled()) {
                    return true;
                }
            }
            return false;
        }
    }
}
