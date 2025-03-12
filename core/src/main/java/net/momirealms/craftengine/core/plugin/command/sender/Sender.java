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

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.util.Tristate;

import java.util.UUID;

public interface Sender {
    UUID CONSOLE_UUID = new UUID(0, 0); // 00000000-0000-0000-0000-000000000000
    String CONSOLE_NAME = "Console";

    Plugin plugin();

    String name();

    UUID uniqueId();

    void sendMessage(Component message);

    void sendMessage(Component message, boolean ignoreEmpty);

    Tristate permissionState(String permission);

    boolean hasPermission(String permission);

    void performCommand(String commandLine);

    boolean isConsole();

    default boolean isValid() {
        return true;
    }

}
