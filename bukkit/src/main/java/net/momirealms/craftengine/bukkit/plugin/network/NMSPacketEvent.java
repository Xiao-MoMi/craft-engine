package net.momirealms.craftengine.bukkit.plugin.network;

import net.momirealms.craftengine.core.util.Cancellable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NMSPacketEvent implements Cancellable {
    private boolean cancelled;
    private boolean hasNewPacket;
    private List<Runnable> delayedTasks = null;
    private final Object packet;
    private Object newPacket;

    public NMSPacketEvent(Object packet) {
        this.packet = packet;
    }

    public Object getPacket() {
        return packet;
    }

    public void setNewPacket(Object newPacket) {
        hasNewPacket = true;
        this.newPacket = newPacket;
    }

    public boolean hasNewPacket() {
        return hasNewPacket;
    }

    @Nullable
    public Object getNewPacket() {
        return newPacket;
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