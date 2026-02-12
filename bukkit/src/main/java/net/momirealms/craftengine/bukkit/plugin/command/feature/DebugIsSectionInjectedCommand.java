package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.injector.WorldStorageInjector;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftChunkProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkAccessProxy;
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
                    Object worldServer = CraftChunkProxy.INSTANCE.getWorld(chunk);
                    Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(worldServer);
                    Object levelChunk;
                    if (VersionHelper.isOrAbove1_21()) {
                        levelChunk = ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedImmediately(chunkSource, chunk.getX(), chunk.getZ());
                    } else {
                        levelChunk = ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedMainThread(chunkSource, chunk.getX(), chunk.getZ());
                    }
                    Object[] sections = ChunkAccessProxy.INSTANCE.getSections(levelChunk);
                    int i = 0;
                    Sender sender = plugin().senderFactory().wrap(player);
                    for (Object section : sections) {
                        sender.sendMessage(Component.text("Section #" + i + ": " + WorldStorageInjector.isSectionInjected(section)));
                        i++;
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_is_section_injected";
    }
}
