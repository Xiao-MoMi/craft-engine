/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
