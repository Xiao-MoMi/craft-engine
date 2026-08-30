package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.injector.WorldStorageInjector;
import net.momirealms.craftengine.bukkit.util.LevelUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkAccessProxy;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

public final class DebugIsSectionInjectedCommand extends BukkitCommandFeature<CommandSender> {

    public DebugIsSectionInjectedCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    Player player = context.sender();
                    Chunk chunk = player.getLocation().getChunk();
                    Object levelChunk = LevelUtils.getMinecraftChunk(chunk);
                    Object[] sections = ChunkAccessProxy.INSTANCE.getSections(levelChunk);
                    int i = 0;
                    Sender sender = plugin().senderFactory().wrap(player);
                    sender.sendMessage(DebugCommandOutput.title("Injected Chunk Sections"));
                    sender.sendMessage(DebugCommandOutput.value("Chunk", chunk.getX() + ", " + chunk.getZ()));
                    for (Object section : sections) {
                        sender.sendMessage(DebugCommandOutput.status("Section #" + i, WorldStorageInjector.isSectionInjected(section)));
                        i++;
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_is_section_injected";
    }
}
