package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

public final class DebugFillSectionCommand extends BukkitCommandFeature<CommandSender> {
    private static final int SECTION_SIZE = 4096;

    public DebugFillSectionCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    Player player = context.sender();
                    Location location = player.getLocation();
                    World world = player.getWorld();
                    int baseX = location.getBlockX() & ~15;
                    int baseY = location.getBlockY() & ~15;
                    int baseZ = location.getBlockZ() & ~15;
                    Sender sender = plugin().senderFactory().wrap(player);
                    if (baseY < world.getMinHeight() || baseY + 15 >= world.getMaxHeight()) {
                        sender.sendMessage(DebugCommandOutput.error("The current section is outside the world build height"));
                        return;
                    }

                    Object level = CraftWorldProxy.INSTANCE.getWorld(world);
                    int index = 0;
                    int changed = 0;
                    for (Object block : (Iterable<?>) BuiltInRegistriesProxy.BLOCK) {
                        if (index == SECTION_SIZE) break;
                        Object state = BlockProxy.INSTANCE.getDefaultBlockState(block);
                        // Internal custom block slots are enumerated through loaded definitions below.
                        if (!BlockStateUtils.isVanillaBlock(state)) continue;
                        if (place(level, baseX, baseY, baseZ, index++, state)) changed++;
                    }
                    int vanillaCount = index;
                    for (BlockDefinition block : plugin().blockManager().loadedBlocks().values()) {
                        if (index == SECTION_SIZE) break;
                        Object state = block.defaultState().customBlockState().minecraftState();
                        if (place(level, baseX, baseY, baseZ, index++, state)) changed++;
                    }

                    sender.sendMessage(DebugCommandOutput.title("Fill Section"));
                    sender.sendMessage(DebugCommandOutput.value("Section", world.getName() + " @ " + (baseX >> 4) + ", " + (baseY >> 4) + ", " + (baseZ >> 4)));
                    sender.sendMessage(DebugCommandOutput.value("Vanilla defaults", vanillaCount));
                    sender.sendMessage(DebugCommandOutput.value("Custom defaults", index - vanillaCount));
                    sender.sendMessage(DebugCommandOutput.value("Changed blocks", changed));
                    sender.sendMessage(DebugCommandOutput.value("Untouched positions", SECTION_SIZE - index));
                });
    }

    private static boolean place(Object level, int baseX, int baseY, int baseZ, int index, Object state) {
        // X advances first, followed by Z and then Y; every position stays in this section.
        Object pos = BlockPosProxy.INSTANCE.newInstance(baseX + (index & 15), baseY + (index >> 8), baseZ + ((index >> 4) & 15));
        return LevelWriterProxy.INSTANCE.setBlock(level, pos, state, UpdateFlags.UPDATE_NO_PHYS | UpdateFlags.UPDATE_SUPPRESS_DROPS);
    }

    @Override
    public String getFeatureID() {
        return "debug_fill_section";
    }
}
