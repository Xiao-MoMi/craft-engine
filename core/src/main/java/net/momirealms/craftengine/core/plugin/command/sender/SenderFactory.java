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

package net.momirealms.craftengine.core.plugin.command.sender;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Tristate;

import java.util.Objects;
import java.util.UUID;

public abstract class SenderFactory<P extends Plugin, T> {
    private final P plugin;

    public SenderFactory(P plugin) {
        this.plugin = plugin;
    }

    protected P plugin() {
        return this.plugin;
    }

    protected abstract UUID uniqueId(T sender);

    protected abstract String name(T sender);

    protected abstract Audience audience(T sender);

    protected abstract void sendMessage(T sender, Component message);

    protected abstract Tristate permissionState(T sender, String node);

    protected abstract boolean hasPermission(T sender, String node);

    protected abstract void performCommand(T sender, String command);

    protected abstract boolean isConsole(T sender);

    protected boolean consoleHasAllPermissions() {
        return true;
    }

    public <C extends T> Sender wrap(C sender) {
        Objects.requireNonNull(sender, "sender");
        return new AbstractSender<>(this.plugin, this, sender);
    }

    public void close() {
    }
}
