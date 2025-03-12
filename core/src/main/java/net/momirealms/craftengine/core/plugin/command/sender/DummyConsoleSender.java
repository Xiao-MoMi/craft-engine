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

import net.momirealms.craftengine.core.plugin.Plugin;

import java.util.UUID;

public abstract class DummyConsoleSender implements Sender {
    private final Plugin platform;

    public DummyConsoleSender(Plugin plugin) {
        this.platform = plugin;
    }

    @Override
    public void performCommand(String commandLine) {
    }

    @Override
    public boolean isConsole() {
        return true;
    }

    @Override
    public Plugin plugin() {
        return this.platform;
    }

    @Override
    public UUID uniqueId() {
        return Sender.CONSOLE_UUID;
    }

    @Override
    public String name() {
        return Sender.CONSOLE_NAME;
    }
}
