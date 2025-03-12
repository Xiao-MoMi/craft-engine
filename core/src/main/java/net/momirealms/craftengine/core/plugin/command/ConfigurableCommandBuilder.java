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

package net.momirealms.craftengine.core.plugin.command;

import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

public interface ConfigurableCommandBuilder<C> {

    ConfigurableCommandBuilder<C> permission(String permission);

    ConfigurableCommandBuilder<C> nodes(String... subNodes);

    Command.Builder<C> build();

    class BasicConfigurableCommandBuilder<C> implements ConfigurableCommandBuilder<C> {
        private Command.Builder<C> commandBuilder;

        public BasicConfigurableCommandBuilder(CommandManager<C> commandManager, String rootNode) {
            this.commandBuilder = commandManager.commandBuilder(rootNode);
        }

        @Override
        public ConfigurableCommandBuilder<C> permission(String permission) {
            this.commandBuilder = this.commandBuilder.permission(permission);
            return this;
        }

        @Override
        public ConfigurableCommandBuilder<C> nodes(String... subNodes) {
            for (String sub : subNodes) {
                this.commandBuilder = this.commandBuilder.literal(sub);
            }
            return this;
        }

        @Override
        public Command.Builder<C> build() {
            return commandBuilder;
        }
    }
}
