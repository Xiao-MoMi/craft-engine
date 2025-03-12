package net.momirealms.craftengine.bukkit.plugin.network;

import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ByteBufPacketEvent implements Cancellable {
    private boolean cancelled;
    private List<Runnable> delayedTasks = null;
    private final FriendlyByteBuf buf;
    private boolean changed;
    private final int packetID;

    public ByteBufPacketEvent(int packetID, FriendlyByteBuf buf) {
        this.buf = buf;
        this.packetID = packetID;
    }

    public int packetID() {
        return packetID;
    }

    public FriendlyByteBuf getBuffer() {
        return buf;
    }

    public void setChanged(boolean dirty) {
        this.changed = dirty;
    }

    public boolean changed() {
        return changed;
    }

    public void addDelayedTask(Runnable task) {
        if (delayedTasks == null) {
            delayedTasks = new ArrayList<>();
        }
        delayedTasks.add(task);
    }

    public List<Runnable> getDelayedTasks() {
        return Optional.ofNullable(delayedTasks).orElse(Collections.emptyList());
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
