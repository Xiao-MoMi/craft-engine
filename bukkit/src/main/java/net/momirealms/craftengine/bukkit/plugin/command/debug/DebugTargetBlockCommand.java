package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.block.entity.render.BlockEntityRenderer;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DebugTargetBlockCommand extends BukkitCommandFeature<CommandSender> {

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
                    sender.sendMessage(Component.text("minecraft state: " + bData)
                            .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                            .clickEvent(ClickEvent.suggestCommand(bData)));
                    Object blockOwner = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(blockState);
                    Object identifier = RegistryProxy.INSTANCE.getKey(BuiltInRegistriesProxy.BLOCK, blockOwner);
                    Object holder = Objects.requireNonNull(RegistryUtils.getHolder(BuiltInRegistriesProxy.BLOCK, ResourceKeyProxy.INSTANCE.create(RegistriesProxy.BLOCK, identifier)));
                    ImmutableBlockState immutableBlockState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
                    if (immutableBlockState != null) {
                        String bState = immutableBlockState.toString();
                        sender.sendMessage(Component.text("craftengine state: " + bState)
                                .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                                .clickEvent(ClickEvent.suggestCommand(bState)));
                        sender.sendMessage(Component.text("visual state: " + immutableBlockState.visualBlockState().getAsString())
                                .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                                .clickEvent(ClickEvent.suggestCommand(immutableBlockState.visualBlockState().getAsString())));
                        sender.sendMessage(Component.text("name: ").append(Component.translatable(block.translationKey())
                                .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                                .clickEvent(ClickEvent.suggestCommand(block.translationKey()))));
                        List<BlockBehavior> behaviors = new ArrayList<>();
                        immutableBlockState.behavior().let(BlockBehavior.class, behaviors::add);
                        if (!behaviors.isEmpty()) {
                            sender.sendMessage(Component.text("behaviors:"));
                            for (BlockBehavior behavior : behaviors) {
                                String name = behavior.getClass().getSimpleName();
                                sender.sendMessage(Component.text("  - " + name)
                                        .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                                        .clickEvent(ClickEvent.suggestCommand(name)));
                            }
                        }
                        CEWorld world = plugin().worldManager().getWorld(block.getWorld().getUID());
                        BlockPos blockPos = LocationUtils.toBlockPos(block.getLocation());
                        ImmutableBlockState dataInCache = world.getBlockStateAtIfLoaded(blockPos);
                        sender.sendMessage(Component.text("storage: " + (dataInCache != null && !dataInCache.isEmpty())));
                        BlockEntity blockEntity = world.getBlockEntityAtIfLoaded(blockPos);
                        if (blockEntity != null) {
                            boolean valid = blockEntity.isValid();
                            sender.sendMessage(Component.text("block entity:"));
                            sender.sendMessage(Component.text("  isValid: " + valid));
                            BlockEntityRenderer renderer = blockEntity.renderer();
                            if (renderer != null) {
                                BlockEntityElement[] elements = renderer.elements();
                                if (elements.length > 0) {
                                    sender.sendMessage(Component.text("  renderer elements:"));
                                    for (BlockEntityElement element : elements) {
                                        String name = element.getClass().getSimpleName();
                                        sender.sendMessage(Component.text("    - " + name)
                                                .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                                                .clickEvent(ClickEvent.suggestCommand(name)));
                                    }
                                }
                            }
                            if (blockEntity.controller != null) {
                                List<BlockEntityController> controllers = new ArrayList<>();
                                blockEntity.controller.let(BlockEntityController.class, controllers::add);
                                if (!controllers.isEmpty()) {
                                    sender.sendMessage(Component.text("  controllers:"));
                                    for (BlockEntityController controller : controllers) {
                                        String name = controller.getClass().getSimpleName();
                                        sender.sendMessage(Component.text("    - " + name)
                                                .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                                                .clickEvent(ClickEvent.suggestCommand(name)));
                                    }
                                }
                            }
                        }
                    }
                    if (HolderProxy.ReferenceProxy.CLASS.isInstance(holder)) {
                        Set<Object> tags = HolderProxy.ReferenceProxy.INSTANCE.getTags(holder);
                        if (!tags.isEmpty()) {
                            sender.sendMessage(Component.text("tags: "));
                            for (Object tag : tags) {
                                String stringTag = TagKeyProxy.INSTANCE.getLocation(tag).toString();
                                sender.sendMessage(Component.text(" - " + stringTag)
                                        .hoverEvent(Component.text("Copy", NamedTextColor.YELLOW))
                                        .clickEvent(ClickEvent.suggestCommand(stringTag)));
                            }
                        }
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_target_block";
    }
}
