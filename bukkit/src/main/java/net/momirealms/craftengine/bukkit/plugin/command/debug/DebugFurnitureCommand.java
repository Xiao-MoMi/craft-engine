package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

public final class DebugFurnitureCommand extends BukkitCommandFeature<CommandSender> {

    public DebugFurnitureCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(context.sender());
                    if (serverPlayer == null) return;
                    boolean b = !serverPlayer.enableFurnitureDebug();
                    serverPlayer.setEnableFurnitureDebug(b);
                    serverPlayer.sendMessage(DebugCommandOutput.title("Furniture"), false);
                    serverPlayer.sendMessage(DebugCommandOutput.status("Debug mode", b), false);
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_furniture";
    }
}
