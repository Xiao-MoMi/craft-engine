package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.block.behavior.StrippableBlockBehavior;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitBlockInWorld;
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
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.GameEvent;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class AxeItemBehavior extends ItemBehavior {
    public static final Factory FACTORY = new Factory();
    public static final AxeItemBehavior INSTANCE = new AxeItemBehavior();
    private static final Key AXE_STRIP_SOUND = Key.of("minecraft:item.axe.strip");

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
        if (player.isAdventureMode()) {
            return InteractionResult.PASS;
        }

        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(context.getLevel().serverWorld(), LocationUtils.toBlockPos(context.getClickedPos()));
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return InteractionResult.PASS;

        ImmutableBlockState customState = optionalCustomState.get();
        Optional<StrippableBlockBehavior> behaviorOptional = customState.behavior().getAs(StrippableBlockBehavior.class);
        if (behaviorOptional.isEmpty()) return InteractionResult.PASS;
        Key stripped = behaviorOptional.get().stripped();
        Item<ItemStack> offHandItem = (Item<ItemStack>) player.getItemInHand(InteractionHand.OFF_HAND);
        // is using a shield
        if (context.getHand() == InteractionHand.MAIN_HAND && offHandItem != null && canBlockAttack(offHandItem) && !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        Optional<CustomBlock> optionalNewCustomBlock = BukkitBlockManager.instance().blockById(stripped);
        if (optionalNewCustomBlock.isEmpty()) {
            CraftEngine.instance().logger().warn("stripped block " + stripped + " does not exist");
            return InteractionResult.FAIL;
        }
        CustomBlock newCustomBlock = optionalNewCustomBlock.get();
        CompoundTag compoundTag = customState.propertiesNbt();
        ImmutableBlockState newState = newCustomBlock.getBlockState(compoundTag);

        org.bukkit.entity.Player bukkitPlayer = ((org.bukkit.entity.Player) player.platformPlayer());

        BukkitBlockInWorld clicked = (BukkitBlockInWorld) context.getLevel().getBlockAt(context.getClickedPos());
        // Call bukkit event
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(bukkitPlayer, clicked.block(), BlockStateUtils.fromBlockData(newState.customBlockState().handle()));
        if (EventUtils.fireAndCheckCancel(event)) {
            return InteractionResult.FAIL;
        }

        Item<ItemStack> item = (Item<ItemStack>) context.getItem();
        BlockPos pos = context.getClickedPos();
        context.getLevel().playBlockSound(Vec3d.atCenterOf(pos), AXE_STRIP_SOUND, 1, 1);
        FastNMS.INSTANCE.method$LevelWriter$setBlock(context.getLevel().serverWorld(), LocationUtils.toBlockPos(pos), newState.customBlockState().handle(), UpdateOption.UPDATE_ALL_IMMEDIATE.flags());
        clicked.block().getWorld().sendGameEvent(bukkitPlayer, GameEvent.BLOCK_CHANGE, new Vector(pos.x(), pos.y(), pos.z()));
        Material material = MaterialUtils.getMaterial(item.vanillaId());
        bukkitPlayer.setStatistic(Statistic.USE_ITEM, material, bukkitPlayer.getStatistic(Statistic.USE_ITEM, material) + 1);

        // resend swing if it's not interactable on client side
        if (!InteractUtils.isInteractable(
                bukkitPlayer, BlockStateUtils.fromBlockData(customState.vanillaBlockState().handle()),
                context.getHitResult(), item
        ) || player.isSecondaryUseActive()) {
            player.swingHand(context.getHand());
        }
        // shrink item amount
        if (VersionHelper.isOrAbove1_20_5()) {
            Object itemStack = item.getLiteralObject();
            Object serverPlayer = player.serverPlayer();
            Object equipmentSlot = context.getHand() == InteractionHand.MAIN_HAND ? CoreReflections.instance$EquipmentSlot$MAINHAND : CoreReflections.instance$EquipmentSlot$OFFHAND;
            try {
                CoreReflections.method$ItemStack$hurtAndBreak.invoke(itemStack, 1, serverPlayer, equipmentSlot);
            } catch (ReflectiveOperationException e) {
                CraftEngine.instance().logger().warn("Failed to hurt itemStack", e);
            }
        } else {
            ItemStack itemStack = item.getItem();
            itemStack.damage(1, bukkitPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    public static class Factory implements ItemBehaviorFactory {
        @Override
        public ItemBehavior create(Pack pack, Path path, Key key, Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
