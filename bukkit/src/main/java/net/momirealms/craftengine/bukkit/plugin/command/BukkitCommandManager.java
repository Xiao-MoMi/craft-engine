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
                new DebugStateCommand(this, plugin),
                new TestCommand(this, plugin),
                new SetBlockCommand(this, plugin)
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
