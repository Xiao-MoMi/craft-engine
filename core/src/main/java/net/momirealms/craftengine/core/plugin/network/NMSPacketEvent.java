package net.momirealms.craftengine.core.plugin.network;

import net.momirealms.craftengine.core.util.Cancellable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NMSPacketEvent implements Cancellable {
    private final Object packet;
    private boolean cancelled;
    private List<Runnable> delayedTasks = null;
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