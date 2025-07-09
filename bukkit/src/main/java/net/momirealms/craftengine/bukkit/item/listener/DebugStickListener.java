package net.momirealms.craftengine.bukkit.item.listener;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DebugStickListener implements Listener {
    private final BukkitCraftEngine plugin;

    public DebugStickListener(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUseDebugStick(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        ItemStack itemInHand = event.getItem();
        if (itemInHand == null) return;
        Material material = itemInHand.getType();
        if (material != Material.DEBUG_STICK) return;
        Player bukkitPlayer = event.getPlayer();
        BukkitServerPlayer player = this.plugin.adapt(bukkitPlayer);
        if (!(player.canInstabuild() && player.hasPermission("minecraft.debugstick")) && !player.hasPermission("minecraft.debugstick.always")) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            int currentTicks = player.gameTicks();
            if (!player.updateLastSuccessfulInteractionTick(currentTicks)) {
                event.setCancelled(true);
                return;
            }
        }
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(FastNMS.INSTANCE.field$CraftWorld$ServerLevel(clickedBlock.getWorld()), LocationUtils.toBlockPos(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()));
        BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(customState -> {
            event.setCancelled(true);
            boolean update = event.getAction() == Action.RIGHT_CLICK_BLOCK;
            CustomBlock block = customState.owner().value();
            Collection<Property<?>> properties = block.properties();
            String blockId = block.id().toString();
            try {
                if (properties.isEmpty()) {
                    Object systemChatPacket = NetworkReflections.constructor$ClientboundSystemChatPacket.newInstance(
                            ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.empty").arguments(Component.text(blockId))), true);
                    player.sendPacket(systemChatPacket, false);
                } else {
                    Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(itemInHand);
                    Object storedData = wrapped.getJavaTag("craftengine:debug_stick_state");
                    if (storedData == null) storedData = new HashMap<>();
                    if (storedData instanceof Map<?,?> map) {
                        Map<String, Object> data = new HashMap<>(MiscUtils.castToMap(map, false));
                        String currentPropertyName = (String) data.get(blockId);
                        Property<?> currentProperty = block.getProperty(currentPropertyName);
                        if (currentProperty == null) {
                            currentProperty = properties.iterator().next();
                        }
                        if (update) {
                            ImmutableBlockState nextState = cycleState(customState, currentProperty, player.isSecondaryUseActive());
                            CraftEngineBlocks.place(clickedBlock.getLocation(), nextState, new UpdateOption.Builder().updateClients().updateKnownShape().build(), false);
                            Object systemChatPacket = NetworkReflections.constructor$ClientboundSystemChatPacket.newInstance(
                                    ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.update")
                                            .arguments(
                                                    Component.text(currentProperty.name()),
                                                    Component.text(getNameHelper(nextState, currentProperty))
                                            )), true);
                            player.sendPacket(systemChatPacket, false);
                        } else {
                            currentProperty = getRelative(properties, currentProperty, player.isSecondaryUseActive());
                            data.put(blockId, currentProperty.name());
                            wrapped.setTag(data, "craftengine:debug_stick_state");
                            Object systemChatPacket = NetworkReflections.constructor$ClientboundSystemChatPacket.newInstance(
                                    ComponentUtils.adventureToMinecraft(Component.translatable("item.minecraft.debug_stick.select")
                                            .arguments(
                                                    Component.text(currentProperty.name()),
                                                    Component.text(getNameHelper(customState, currentProperty))
                                            )), true);
                            player.sendPacket(systemChatPacket, false);
                        }
                    }
                }
            } catch (ReflectiveOperationException e) {
                this.plugin.logger().warn("Failed to send system chat packet", e);
            }
        });
    }

    private static <T extends Comparable<T>> ImmutableBlockState cycleState(ImmutableBlockState state, Property<T> property, boolean inverse) {
        return state.with(property, getRelative(property.possibleValues(), state.get(property), inverse));
    }

    private static <T> T getRelative(Iterable<T> elements, @Nullable T current, boolean inverse) {
        return inverse ? MCUtils.findPreviousInIterable(elements, current) : MCUtils.findNextInIterable(elements, current);
    }

    private static <T extends Comparable<T>> String getNameHelper(ImmutableBlockState state, Property<T> property) {
        return property.valueName(state.get(property));
    }
}
