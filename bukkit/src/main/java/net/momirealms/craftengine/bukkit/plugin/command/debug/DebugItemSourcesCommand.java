package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.plugin.compatibility.ItemSource;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;

import java.util.List;

public final class DebugItemSourcesCommand extends BukkitCommandFeature<CommandSender> {

    public DebugItemSourcesCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder.handler(context -> {
            Sender sender = plugin().senderFactory().wrap(context.sender());
            List<ItemSource> itemSources = plugin().compatibilityManager().itemSources();
            sender.sendMessage(DebugCommandOutput.title("Item Sources"));
            sender.sendMessage(DebugCommandOutput.value("Count", itemSources.size()));
            sender.sendMessage(DebugCommandOutput.section("Effective order"));
            if (itemSources.isEmpty()) {
                sender.sendMessage(DebugCommandOutput.empty(2));
                return;
            }
            for (ItemSource itemSource : itemSources) {
                sender.sendMessage(DebugCommandOutput.listItem(2, itemSource.plugin()));
            }
        });
    }

    @Override
    public String getFeatureID() {
        return "debug_item_sources";
    }
}
