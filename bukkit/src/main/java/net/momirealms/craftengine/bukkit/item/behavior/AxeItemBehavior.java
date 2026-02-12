package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.behavior.StrippableBlockBehavior;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.GameEvent;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public final class AxeItemBehavior extends ItemBehavior {
    public static final ItemBehaviorFactory<AxeItemBehavior> FACTORY = new Factory();
    public static final AxeItemBehavior INSTANCE = new AxeItemBehavior();
    private static final Key AXE_STRIP_SOUND = Key.of("minecraft:item.axe.strip");

    private AxeItemBehavior() {}

    private boolean canBlockAttack(Item<ItemStack> item) {
        if (VersionHelper.isOrAbove1_21_5()) {
            return item.hasComponent("minecraft:blocks_attacks");
        } else {
            return item.vanillaId().equals(ItemKeys.SHIELD);
        }
    }

    @SuppressWarnings({"UnstableApiUsage", "unchecked"})
    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        Player player = context.getPlayer();
        // no adventure mode for the moment
        if (player != null && player.isAdventureMode()) {
            return InteractionResult.PASS;
        }

        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(context.getLevel().serverWorld(), LocationUtils.toBlockPos(context.getClickedPos()));
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return InteractionResult.PASS;

        ImmutableBlockState customState = optionalCustomState.get();
        Optional<StrippableBlockBehavior> behaviorOptional = customState.behavior().getAs(StrippableBlockBehavior.class);
        if (behaviorOptional.isEmpty()) return InteractionResult.PASS;
        Item<ItemStack> offHandItem = player != null ? (Item<ItemStack>) player.getItemInHand(InteractionHand.OFF_HAND) : BukkitItemManager.instance().uniqueEmptyItem().item();
        // is using a shield
        if (context.getHand() == InteractionHand.MAIN_HAND && !ItemUtils.isEmpty(offHandItem) && canBlockAttack(offHandItem) && player != null && !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        BlockStateWrapper newState = behaviorOptional.get().strippedState();
        if (newState == null) {
            CraftEngine.instance().logger().warn("stripped block " + behaviorOptional.get().stripped() + " does not exist");
            return InteractionResult.FAIL;
        }

        newState = newState.withProperties(behaviorOptional.get().filter(customState.propertiesNbt()));
        BukkitExistingBlock clicked = (BukkitExistingBlock) context.getLevel().getBlock(context.getClickedPos());
        org.bukkit.entity.Player bukkitPlayer = null;
        if (player != null) {
            bukkitPlayer = ((org.bukkit.entity.Player) player.platformPlayer());
            // Call bukkit event
            EntityChangeBlockEvent event = new EntityChangeBlockEvent(bukkitPlayer, clicked.block(), BlockStateUtils.fromBlockData(newState.literalObject()));
            if (EventUtils.fireAndCheckCancel(event)) {
                return InteractionResult.FAIL;
            }
        }

        Item<ItemStack> item = (Item<ItemStack>) context.getItem();
        // 理论不可能出现
        if (ItemUtils.isEmpty(item)) return InteractionResult.FAIL;
        BlockPos pos = context.getClickedPos();
        context.getLevel().playBlockSound(Vec3d.atCenterOf(pos), AXE_STRIP_SOUND, 1, 1);
        FastNMS.INSTANCE.method$LevelWriter$setBlock(context.getLevel().serverWorld(), LocationUtils.toBlockPos(pos), newState.literalObject(), UpdateOption.UPDATE_ALL_IMMEDIATE.flags());
        clicked.block().getWorld().sendGameEvent(bukkitPlayer, GameEvent.BLOCK_CHANGE, new Vector(pos.x(), pos.y(), pos.z()));
        Material material = MaterialUtils.getMaterial(item.vanillaId());
        if (bukkitPlayer != null) {
            bukkitPlayer.setStatistic(Statistic.USE_ITEM, material, bukkitPlayer.getStatistic(Statistic.USE_ITEM, material) + 1);

            // resend swing if it's not interactable on client side
            if (!InteractUtils.isInteractable(
                    bukkitPlayer, BlockStateUtils.fromBlockData(customState.visualBlockState().literalObject()),
                    context.getHitResult(), item
            ) || player.isSecondaryUseActive()) {
                player.swingHand(context.getHand());
            }
            // shrink item amount
            item.hurtAndBreak(1, player, context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    private static class Factory implements ItemBehaviorFactory<AxeItemBehavior> {
        @Override
        public AxeItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
