package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("Effective item sources (" + itemSources.size() + "):").color(NamedTextColor.GRAY));
            if (itemSources.isEmpty()) {
                sender.sendMessage(Component.text("- none").color(NamedTextColor.DARK_GRAY));
                return;
            }
            for (ItemSource itemSource : itemSources) {
                sender.sendMessage(Component.text("- ").color(NamedTextColor.DARK_GRAY)
                        .append(Component.text(itemSource.plugin()).color(NamedTextColor.AQUA)));
            }
        });
    }

    @Override
    public String getFeatureID() {
        return "debug_item_sources";
    }
}
