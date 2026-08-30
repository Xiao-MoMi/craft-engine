package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

public final class DebugIsChunkPersistentLoadedCommand extends BukkitCommandFeature<CommandSender> {

    public DebugIsChunkPersistentLoadedCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    Player player = context.sender();
                    Chunk chunk = player.getLocation().getChunk();
                    Sender sender = plugin().senderFactory().wrap(player);
                    if (VersionHelper.hasFoliaPatch) {
                        sender.sendMessage(DebugCommandOutput.error("Persistent chunk state is unavailable on Folia"));
                        return;
                    }
                    sender.sendMessage(DebugCommandOutput.title("Persistent Chunk"));
                    sender.sendMessage(DebugCommandOutput.value("Chunk", chunk.getX() + ", " + chunk.getZ()));
                    sender.sendMessage(DebugCommandOutput.status("Force loaded", chunk.isForceLoaded()));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_is_chunk_persistent_loaded";
    }
}
