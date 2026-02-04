package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

import java.util.Set;

public class DebugTargetBlockCommand extends BukkitCommandFeature<CommandSender> {

    public DebugTargetBlockCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .flag(manager.flagBuilder("this").build())
                .handler(context -> {
                    Player player = context.sender();
                    Block block;
                    if (context.flags().hasFlag("this")) {
                        Location location = player.getLocation();
                        block = location.getBlock();
                    } else {
                        block = player.getTargetBlockExact(10);
                        if (block == null) return;
                    }
                    String bData = block.getBlockData().getAsString();
                    Object blockState = BlockStateUtils.blockDataToBlockState(block.getBlockData());
                    Sender sender = plugin().senderFactory().wrap(context.sender());
                    sender.sendMessage(Component.text("real state: " + bData)
                            .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                            .clickEvent(ClickEvent.suggestCommand(bData)));
                    int id = BlockStateUtils.blockStateToId(blockState);
                    if (!BlockStateUtils.isVanillaBlock(id)) {
                        Object holder = BukkitBlockManager.instance().getMinecraftBlockHolder(id);
                        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(id);
                        if (immutableBlockState != null) {
                            String bState = immutableBlockState.toString();
                            sender.sendMessage(Component.text("config state: " + bState)
                                    .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                                    .clickEvent(ClickEvent.suggestCommand(bState)));
                            sender.sendMessage(Component.text("visual state: " + immutableBlockState.visualBlockState().getAsString())
                                    .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                                    .clickEvent(ClickEvent.suggestCommand(immutableBlockState.visualBlockState().getAsString())));
                        }
                        sender.sendMessage(Component.text("name: ").append(Component.translatable(block.translationKey())));
                        ImmutableBlockState dataInCache = plugin().worldManager().getWorld(block.getWorld().getUID()).getBlockStateAtIfLoaded(LocationUtils.toBlockPos(block.getLocation()));
                        sender.sendMessage(Component.text("storage: " + (dataInCache != null && !dataInCache.isEmpty())));
                        try {
                            @SuppressWarnings("unchecked")
                            Set<Object> tags = (Set<Object>) CoreReflections.field$Holder$Reference$tags.get(holder);
                            if (!tags.isEmpty()) {
                                sender.sendMessage(Component.text("tags: "));
                                for (Object tag : tags) {
                                    sender.sendMessage(Component.text(" - " + CoreReflections.field$TagKey$location.get(tag).toString()));
                                }
                            }
                        } catch (ReflectiveOperationException e) {
                            plugin().logger().warn("Could not get tags", e);
                        }
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_target_block";
    }
}
