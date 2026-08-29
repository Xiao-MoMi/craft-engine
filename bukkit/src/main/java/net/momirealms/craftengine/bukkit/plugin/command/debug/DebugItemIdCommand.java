package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

public final class DebugItemIdCommand extends BukkitCommandFeature<CommandSender> {

    public DebugItemIdCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    BukkitServerPlayer player = BukkitAdaptor.adapt(context.sender());
                    if (player == null) return;

                    Item item = player.getItemInHand(InteractionHand.MAIN_HAND);
                    var sender = plugin().senderFactory().wrap(context.sender());
                    if (item.isEmpty()) {
                        sender.sendMessage(DebugCommandOutput.error("The main hand is empty"));
                        return;
                    }

                    sender.sendMessage(DebugCommandOutput.title("Item ID"));
                    sender.sendMessage(DebugCommandOutput.value("ID", item.id()));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_item_id";
    }
}
