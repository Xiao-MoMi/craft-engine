package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.injector.BukkitInjector;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

public class DebugIsSectionInjectedCommand extends BukkitCommandFeature<CommandSender> {

    public DebugIsSectionInjectedCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    Player player = context.sender();
                    Chunk chunk = player.getChunk();
                    Object worldServer = FastNMS.INSTANCE.field$CraftChunk$worldServer(chunk);
                    Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(worldServer);
                    Object levelChunk = FastNMS.INSTANCE.method$ServerChunkCache$getChunkAtIfLoadedMainThread(chunkSource, chunk.getX(), chunk.getZ());
                    Object[] sections = FastNMS.INSTANCE.method$ChunkAccess$getSections(levelChunk);
                    int i = 0;
                    Sender sender = plugin().senderFactory().wrap(player);
                    for (Object section : sections) {
                        sender.sendMessage(Component.text("Section #" + i + ": " + BukkitInjector.isSectionInjected(section)));
                        i++;
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_is_section_injected";
    }
}
