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

package net.momirealms.craftengine.bukkit.plugin.command;

import net.kyori.adventure.util.Index;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.command.feature.*;
import net.momirealms.craftengine.core.plugin.command.AbstractCommandManager;
import net.momirealms.craftengine.core.plugin.command.CommandFeature;
import net.momirealms.craftengine.core.plugin.command.parser.BlockStateParser;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.setting.ManagerSetting;

import java.util.List;

public class BukkitCommandManager extends AbstractCommandManager<CommandSender> {
    private final BukkitCraftEngine plugin;
    private final Index<String, CommandFeature<CommandSender>> INDEX;

    public BukkitCommandManager(BukkitCraftEngine plugin) {
        super(plugin, new LegacyPaperCommandManager<>(
                plugin.bootstrap(),
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
        ));
        this.plugin = plugin;
        this.INDEX = Index.create(CommandFeature::getFeatureID, List.of(
                new ReloadCommand(this, plugin),
                new GetItemCommand(this, plugin),
                new GiveItemCommand(this, plugin),
                new ItemBrowserCommand(this, plugin),
                new TestCommand(this, plugin),
                new DebugGetBlockStateRegistryIdCommand(this, plugin),
                new DebugGetBlockInternalIdCommand(this, plugin),
                new DebugAppearanceStateUsageCommand(this, plugin),
                new DebugRealStateUsageCommand(this, plugin),
                new DebugItemDataCommand(this, plugin),
                new DebugSetBlockCommand(this, plugin),
                new DebugTargetBlockCommand(this, plugin)
        ));
        final LegacyPaperCommandManager<CommandSender> manager = (LegacyPaperCommandManager<CommandSender>) getCommandManager();
        manager.settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true);
        manager.parserRegistry().registerParser(BlockStateParser.blockStateParser());
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            manager.registerBrigadier();
            manager.brigadierManager().setNativeNumberSuggestions(true);
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }
    }

    @Override
    protected Sender wrapSender(CommandSender sender) {
        return plugin.senderFactory().wrap(sender);
    }

    @Override
    public Index<String, CommandFeature<CommandSender>> features() {
        return INDEX;
    }
}
