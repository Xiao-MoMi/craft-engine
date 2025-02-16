package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.block.behavior.StrippableBlockBehavior;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.EventUtils;
import net.momirealms.craftengine.bukkit.util.MaterialUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorldBlock;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.GameEvent;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Optional;

public class AxeItemBehavior extends ItemBehavior {
    public static final Factory FACTORY = new Factory();
    public static final AxeItemBehavior INSTANCE = new AxeItemBehavior();
    private static final Key AXE_STRIP_SOUND = Key.of("minecraft:item.axe.strip");

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        BukkitWorldBlock clicked = (BukkitWorldBlock) context.getLevel().getBlockAt(context.getClickedPos());
        Block block = clicked.block();
        ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockDataToId(block.getBlockData()));
        if (state == null || state.isEmpty()) return InteractionResult.PASS;

        if (!(state.behavior() instanceof StrippableBlockBehavior blockBehavior)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        @SuppressWarnings("unchecked")
        Item<ItemStack> offHandItem = (Item<ItemStack>) player.getItemInHand(InteractionHand.OFF_HAND);
        // is using a shield
        if (context.getHand() == InteractionHand.MAIN_HAND && offHandItem != null && offHandItem.vanillaId().equals(ItemKeys.SHIELD) && !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        Optional<CustomBlock> optionalNewCustomBlock = BukkitBlockManager.instance().getBlock(blockBehavior.stripped());
        if (optionalNewCustomBlock.isEmpty()) {
            CraftEngine.instance().logger().warn("stripped block " + blockBehavior.stripped() + " does not exist");
            return InteractionResult.FAIL;
        }
        CustomBlock newCustomBlock = optionalNewCustomBlock.get();
        CompoundTag compoundTag = state.propertiesNbt();
        ImmutableBlockState newState = newCustomBlock.getBlockState(compoundTag);

        org.bukkit.entity.Player bukkitPlayer = ((org.bukkit.entity.Player) player.platformPlayer());
        // Call bukkit event
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(bukkitPlayer, block, BlockStateUtils.createBlockData(newState.customBlockState().handle()));
        if (EventUtils.fireAndCheckCancel(event)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        context.getLevel().playBlockSound(Vec3d.atCenterOf(pos), AXE_STRIP_SOUND, 1, 1);
        CraftEngineBlocks.place(block.getLocation(), newState, UpdateOption.UPDATE_ALL_IMMEDIATE, false);
        block.getWorld().sendGameEvent(bukkitPlayer, GameEvent.BLOCK_CHANGE, new Vector(pos.x(), pos.y(), pos.z()));
        Item<?> item = context.getItem();
        Material material = MaterialUtils.getMaterial(item.vanillaId());
        bukkitPlayer.setStatistic(Statistic.USE_ITEM, material, bukkitPlayer.getStatistic(Statistic.USE_ITEM, material) + 1);

        if (VersionHelper.isVersionNewerThan1_20_5()) {
            Object itemStack = item.getLiteralObject();
            Object serverPlayer = player.serverPlayer();
            Object equipmentSlot = context.getHand() == InteractionHand.MAIN_HAND ? Reflections.instance$EquipmentSlot$MAINHAND : Reflections.instance$EquipmentSlot$OFFHAND;
            try {
                Reflections.method$ItemStack$hurtAndBreak.invoke(itemStack, 1, serverPlayer, equipmentSlot);
            } catch (ReflectiveOperationException e) {
                CraftEngine.instance().logger().warn("Failed to hurt itemStack", e);
            }
        } else {
            ItemStack itemStack = (ItemStack) item.getItem();
            itemStack.damage(1, bukkitPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    public static class Factory implements ItemBehaviorFactory {

        @Override
        public ItemBehavior create(Key id, Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
