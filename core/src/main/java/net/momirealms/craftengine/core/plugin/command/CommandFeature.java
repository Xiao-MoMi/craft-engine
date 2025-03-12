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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.momirealms.craftengine.core.plugin.Plugin;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;

public interface CommandFeature<C> {

    Command<C> registerCommand(org.incendo.cloud.CommandManager<C> cloudCommandManager, Command.Builder<C> builder);

    String getFeatureID();

    void registerRelatedFunctions();

    void unregisterRelatedFunctions();

    void handleFeedback(CommandContext<?> context, TranslatableComponent.Builder key, Component... args);

    void handleFeedback(C sender, TranslatableComponent.Builder key, Component... args);

    CraftEngineCommandManager<C> commandManager();

    CommandConfig<C> commandConfig();

    Plugin plugin();
}
