package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
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
import org.bukkit.block.data.BlockData;
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
                    Sender sender = plugin().senderFactory().wrap(context.sender());
                    Block block;
                    if (context.flags().hasFlag("this")) {
                        Location location = player.getLocation();
                        block = location.getBlock();
                    } else {
                        block = player.getTargetBlockExact(10);
                        if (block == null) {
                            sender.sendMessage(DebugCommandOutput.error("No block found within 10 blocks"));
                            return;
                        }
                    }
                    BlockData blockData = block.getBlockData();
                    String bData = blockData.getAsString();
                    Object blockState = BlockStateUtils.blockDataToBlockState(blockData);
                    sender.sendMessage(DebugCommandOutput.title("Target Block"));
                    sender.sendMessage(DebugCommandOutput.value("Location", formatLocation(block.getLocation())));
                    sender.sendMessage(DebugCommandOutput.section("Minecraft"));
                    sender.sendMessage(DebugCommandOutput.value(2, "State", bData));
                    Object blockOwner = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(blockState);
                    Object identifier = RegistryProxy.INSTANCE.getKey(BuiltInRegistriesProxy.BLOCK, blockOwner);
                    Object holder = Objects.requireNonNull(RegistryUtils.getHolder(BuiltInRegistriesProxy.BLOCK, ResourceKeyProxy.INSTANCE.create(RegistriesProxy.BLOCK, identifier)));
                    String descriptionId = BlockStateUtils.getDescriptionId(blockState);
                    Component translatedName = Component.translatable(descriptionId, DebugCommandOutput.accentColor())
                            .hoverEvent(Component.translatable("chat.copy.click", DebugCommandOutput.textColor()))
                            .clickEvent(ClickEvent.copyToClipboard(descriptionId));
                    sender.sendMessage(DebugCommandOutput.value(2, "Name", translatedName));
                    ImmutableBlockState immutableBlockState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
                    if (immutableBlockState != null) {
                        sender.sendMessage(DebugCommandOutput.section("Custom"));
                        String bState = immutableBlockState.toString();
                        sender.sendMessage(DebugCommandOutput.value(2, "State", bState));
                        sender.sendMessage(DebugCommandOutput.value(2, "Visual state", immutableBlockState.visualBlockState().getAsString()));
                        List<BlockBehavior> behaviors = new ArrayList<>();
                        immutableBlockState.behavior().let(BlockBehavior.class, behaviors::add);
                        sender.sendMessage(DebugCommandOutput.section(2, "Behaviors (" + behaviors.size() + ")"));
                        if (behaviors.isEmpty()) {
                            sender.sendMessage(DebugCommandOutput.empty(3));
                        } else {
                            for (BlockBehavior behavior : behaviors) {
                                String name = behavior.getClass().getSimpleName();
                                sender.sendMessage(DebugCommandOutput.listItem(3, name));
                            }
                        }
                        CEWorld world = BukkitAdaptor.adapt(block.getWorld()).storageWorld();
                        BlockPos blockPos = LocationUtils.toBlockPos(block.getLocation());
                        BlockEntity blockEntity = world.getBlockEntityAtIfLoaded(blockPos);
                        if (blockEntity != null) {
                            boolean valid = blockEntity.isValid();
                            sender.sendMessage(DebugCommandOutput.section(2, "Block Entity"));
                            sender.sendMessage(DebugCommandOutput.value(3, "Valid", DebugCommandOutput.booleanValue(valid)));
                            BlockEntityRenderer renderer = blockEntity.dynamicRenderer();
                            if (renderer != null) {
                                BlockEntityElement[] elements = renderer.elements();
                                if (elements.length > 0) {
                                    sender.sendMessage(DebugCommandOutput.section(3, "Renderer Elements (" + elements.length + ")"));
                                    for (BlockEntityElement element : elements) {
                                        String name = element.getClass().getSimpleName();
                                        sender.sendMessage(DebugCommandOutput.listItem(4, name));
                                    }
                                }
                            }
                            if (blockEntity.controller != null) {
                                List<BlockEntityController> controllers = new ArrayList<>();
                                blockEntity.controller.let(BlockEntityController.class, controllers::add);
                                if (!controllers.isEmpty()) {
                                    sender.sendMessage(DebugCommandOutput.section(3, "Controllers (" + controllers.size() + ")"));
                                    for (BlockEntityController controller : controllers) {
                                        String name = controller.getClass().getSimpleName();
                                        sender.sendMessage(DebugCommandOutput.listItem(4, name));
                                    }
                                }
                            }
                        }
                    }
                    if (HolderProxy.ReferenceProxy.CLASS.isInstance(holder)) {
                        Set<Object> tags = HolderProxy.ReferenceProxy.INSTANCE.getTags(holder);
                        sender.sendMessage(DebugCommandOutput.section("Tags (" + tags.size() + ")"));
                        if (tags.isEmpty()) {
                            sender.sendMessage(DebugCommandOutput.empty(2));
                        } else {
                            for (Object tag : tags) {
                                String stringTag = TagKeyProxy.INSTANCE.getLocation(tag).toString();
                                sender.sendMessage(DebugCommandOutput.listItem(2, stringTag));
                            }
                        }
                        CEWorld world = BukkitAdaptor.adapt(block.getWorld()).storageWorld();
                        BlockPos blockPos = LocationUtils.toBlockPos(block.getLocation());
                        ImmutableBlockState dataInCache = world.getBlockStateAtIfLoaded(blockPos);
                        sender.sendMessage(DebugCommandOutput.status("Stored by CraftEngine", dataInCache != null && !dataInCache.isEmpty()));
                    }
                });
    }

    private static String formatLocation(Location location) {
        return location.getWorld().getName() + " @ " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    @Override
    public String getFeatureID() {
        return "debug_target_block";
    }
}
