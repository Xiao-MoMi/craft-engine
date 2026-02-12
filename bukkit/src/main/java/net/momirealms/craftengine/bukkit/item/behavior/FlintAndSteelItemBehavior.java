package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.InteractUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
import java.util.Map;

public final class FlintAndSteelItemBehavior extends ItemBehavior {
    public static final FlintAndSteelItemBehavior INSTANCE = new FlintAndSteelItemBehavior();
    public static final ItemBehaviorFactory<FlintAndSteelItemBehavior> FACTORY = new Factory();
    private static final Key FLINT_SOUND = Key.of("item.flintandsteel.use");

    private FlintAndSteelItemBehavior() {}

    @SuppressWarnings("unchecked")
    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        net.momirealms.craftengine.core.entity.player.Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos clickedPos = context.getClickedPos();
        BukkitExistingBlock clicked = (BukkitExistingBlock) context.getLevel().getBlock(clickedPos);
        Block block = clicked.block();
        BlockPos firePos = clickedPos.relative(context.getClickedFace());
        Direction direction = context.getHorizontalDirection();

        // 最基础的判断能不能着火，不能着火都是扯蛋
        try {
            if (!(boolean) CoreReflections.method$BaseFireBlock$canBePlacedAt.invoke(null, context.getLevel().serverWorld(), LocationUtils.toBlockPos(firePos), DirectionUtils.toNMSDirection(direction))) {
                return InteractionResult.PASS;
            }
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to call BaseFireBlock$canBePlacedAt", e);
            return InteractionResult.PASS;
        }

        // 判断点击的方块是否可燃
        BlockData clickedBlockData = block.getBlockData();
        Object clickedBlockState = BlockStateUtils.blockDataToBlockState(clickedBlockData);
        boolean isClickedBlockBurnable;
        try {
            isClickedBlockBurnable = BlockStateUtils.isBurnable(clickedBlockState) ||
                    (context.getClickedFace() == Direction.UP && (boolean) CoreReflections.method$BlockStateBase$isFaceSturdy.invoke(
                            clickedBlockState, context.getLevel().serverWorld(), LocationUtils.toBlockPos(clickedPos), CoreReflections.instance$Direction$UP, CoreReflections.instance$SupportType$FULL));
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to call method$BlockStateBase$isFaceSturdy", e);
            return InteractionResult.PASS;
        }

        // 点击对象直接可燃，则忽略
        if (isClickedBlockBurnable) {
            int stateId = BlockStateUtils.blockStateToId(clickedBlockState);
            if (BlockStateUtils.isVanillaBlock(stateId)) {
                return InteractionResult.PASS;
            } else {
                // 点击对象为自定义方块
                ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(stateId);
                // 原版外观也可燃
                if (BlockStateUtils.isBurnable(immutableBlockState.visualBlockState().literalObject())) {
                    return InteractionResult.PASS;
                }
                BlockData vanillaBlockState = BlockStateUtils.fromBlockData(immutableBlockState.visualBlockState().literalObject());
                // 点击的是方块上面，则只需要判断shift和可交互
                if (direction == Direction.UP) {
                    // 客户端层面必须可交互
                    if (!InteractUtils.isInteractable((Player) player.platformPlayer(), vanillaBlockState,
                            context.getHitResult(), (Item<ItemStack>) context.getItem())) {
                        return InteractionResult.PASS;
                    }
                    // 且没有shift或者忽略潜行的可交互方块
                    if (!player.isSecondaryUseActive()) {
                        player.playSound(firePos, FLINT_SOUND, SoundSource.BLOCK, 1f, RandomUtils.generateRandomFloat(0.8f, 1.2f));
                    }
                } else {
                    // 玩家觉得自定义方块不可燃，且点击了侧面，那么就要判断火源下方的方块是否可燃，如果不可燃，则补发声音
                    BlockPos belowFirePos = firePos.relative(Direction.DOWN);
                    BukkitExistingBlock belowFireBlock = (BukkitExistingBlock) context.getLevel().getBlock(belowFirePos);
                    boolean belowCanBurn;
                    try {
                        Block belowBlock = belowFireBlock.block();
                        belowCanBurn = BlockStateUtils.isBurnable(BlockStateUtils.blockDataToBlockState(belowBlock.getBlockData())) ||
                                (boolean) CoreReflections.method$BlockStateBase$isFaceSturdy.invoke(
                                        BlockStateUtils.blockDataToBlockState(belowFireBlock.block().getBlockData()), context.getLevel().serverWorld(), LocationUtils.toBlockPos(belowFirePos), CoreReflections.instance$Direction$UP, CoreReflections.instance$SupportType$FULL);
                    } catch (ReflectiveOperationException e) {
                        CraftEngine.instance().logger().warn("Failed to call method$BlockStateBase$isFaceSturdy", e);
                        return InteractionResult.PASS;
                    }

                    // 客户端觉得这玩意可交互，就会忽略声音
                    if (InteractUtils.isInteractable((Player) player.platformPlayer(), vanillaBlockState, context.getHitResult(), (Item<ItemStack>) context.getItem())) {
                        // 如果按住了shift，则代表尝试对侧面方块点火
                        if (player.isSecondaryUseActive()) {
                            // 如果底部不能燃烧，则燃烧点位为侧面，需要补发
                            if (!belowCanBurn) {
                                player.playSound(firePos, FLINT_SOUND, SoundSource.BLOCK, 1f, RandomUtils.generateRandomFloat(0.8f, 1.2f));
                                player.swingHand(context.getHand());
                            }
                        } else {
                            player.playSound(firePos, FLINT_SOUND, SoundSource.BLOCK, 1f, RandomUtils.generateRandomFloat(0.8f, 1.2f));
                        }
                    } else {
                        // 如果底部方块不可燃烧才补发
                        if (!belowCanBurn) {
                            player.playSound(firePos, FLINT_SOUND, SoundSource.BLOCK, 1f, RandomUtils.generateRandomFloat(0.8f, 1.2f));
                            player.swingHand(context.getHand());
                        }
                    }
                }
            }
        } else {
            // 如果点击的方块不可燃烧，但是服务端却认为可以放置火源，则可燃烧的方块一定位于火源的六个方向之一。
            Direction relativeDirection = direction.opposite();
            for (Direction dir : Direction.values()) {
                if (dir == relativeDirection) continue;
                BlockPos relPos = firePos.relative(dir);
                BukkitExistingBlock nearByBlock = (BukkitExistingBlock) context.getLevel().getBlock(relPos);
                BlockData nearbyBlockData = nearByBlock.block().getBlockData();
                Object nearbyBlockState = BlockStateUtils.blockDataToBlockState(nearbyBlockData);
                int stateID = BlockStateUtils.blockStateToId(nearbyBlockState);
                if (BlockStateUtils.isVanillaBlock(stateID)) {
                    if (BlockStateUtils.isBurnable(nearbyBlockState)) {
                        return InteractionResult.PASS;
                    }
                    try {
                        if (dir == Direction.DOWN && (boolean) CoreReflections.method$BlockStateBase$isFaceSturdy.invoke(
                                nearbyBlockState, context.getLevel().serverWorld(), LocationUtils.toBlockPos(relPos), CoreReflections.instance$Direction$UP, CoreReflections.instance$SupportType$FULL)) {
                            return InteractionResult.PASS;
                        }
                    } catch (ReflectiveOperationException e) {
                        CraftEngine.instance().logger().warn("Failed to call method$BlockStateBase$isFaceSturdy", e);
                        return InteractionResult.PASS;
                    }
                }
            }
            player.playSound(firePos, FLINT_SOUND, SoundSource.BLOCK, 1f, RandomUtils.generateRandomFloat(0.8f, 1.2f));
            player.swingHand(context.getHand());
        }
        return InteractionResult.PASS;
    }

    private static class Factory implements ItemBehaviorFactory<FlintAndSteelItemBehavior> {
        @Override
        public FlintAndSteelItemBehavior create(Pack pack, Path path, String node, Key id, Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
