package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

public final class ItemBrowserPlayerCommand extends BukkitCommandFeature<CommandSender> {

    public ItemBrowserPlayerCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    Player player = context.sender();
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
                    if (serverPlayer == null) return;
                    plugin().itemBrowserManager().open(serverPlayer);
                });
    }

    @Override
    public String getFeatureID() {
        return "item_browser_player";
    }
}
