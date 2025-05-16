package net.momirealms.craftengine.core.plugin.network;

import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public class ByteBufPacketEvent implements Cancellable {
    private boolean cancelled;
    private final FriendlyByteBuf buf;
    private boolean changed;
    private final int packetID;
    private final int preIndex;

    public ByteBufPacketEvent(int packetID, FriendlyByteBuf buf, int preIndex) {
        this.buf = buf;
        this.packetID = packetID;
        this.preIndex = preIndex;
    }

    public int packetID() {
        return this.packetID;
    }

    public FriendlyByteBuf getBuffer() {
        this.buf.readerIndex(this.preIndex);
        return this.buf;
    }

    public void setChanged(boolean dirty) {
        this.changed = dirty;
    }

    public boolean changed() {
        return this.changed;
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
