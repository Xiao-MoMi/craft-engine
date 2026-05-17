package net.momirealms.craftengine.core.plugin.network.event;

import net.momirealms.craftengine.core.util.Cancellable;
import org.jetbrains.annotations.NotNull;

public final class NMSPacketEvent implements Cancellable {
    private final Object packet;
    private boolean cancelled;
    private Object newPacket = null;

    public NMSPacketEvent(Object packet) {
        this.packet = packet;
    }

    public Object getPacket() {
        return packet;
    }

    public void replacePacket(@NotNull Object newPacket) {
        this.newPacket = newPacket;
    }

    public boolean isUsingNewPacket() {
        return newPacket != null;
    }

    public Object optionalNewPacket() {
        return newPacket;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}