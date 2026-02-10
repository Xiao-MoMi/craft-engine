package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ClipContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.HitResultProxy;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;

public final class LiquidCollisionBlockItemBehavior extends BlockItemBehavior {
    public static final ItemBehaviorFactory<LiquidCollisionBlockItemBehavior> FACTORY = new Factory();
    private final int offsetY;

    private LiquidCollisionBlockItemBehavior(Key blockId, int offsetY) {
        super(blockId);
        this.offsetY = offsetY;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }

    @Override
    public InteractionResult use(World world, @Nullable Player player, InteractionHand hand) {
        try {
            if (player == null) return InteractionResult.FAIL;
            Object blockHitResult = ItemProxy.INSTANCE.getPlayerPOVHitResult(world.serverWorld(), player.serverPlayer(), ClipContextProxy.FluidProxy.SOURCE_ONLY);
            Object blockPos = BlockHitResultProxy.INSTANCE.getBlockPos(blockHitResult);
            BlockPos above = new BlockPos(FastNMS.INSTANCE.field$Vec3i$x(blockPos), FastNMS.INSTANCE.field$Vec3i$y(blockPos) + offsetY, FastNMS.INSTANCE.field$Vec3i$z(blockPos));
            Direction direction = DirectionUtils.fromNMSDirection(BlockHitResultProxy.INSTANCE.getDirection(blockHitResult));
            boolean miss = BlockHitResultProxy.INSTANCE.isMiss(blockHitResult);
            Vec3d hitPos = LocationUtils.fromVec(HitResultProxy.INSTANCE.getLocation(blockHitResult));
            Object fluidType = FluidStateProxy.INSTANCE.getType(BlockGetterProxy.INSTANCE.getFluidState(world.serverWorld(), blockPos));
            if (fluidType != MFluids.WATER && fluidType != MFluids.LAVA) {
                return InteractionResult.PASS;
            }
            if (miss) {
                return super.useOnBlock(new UseOnContext(player, hand, BlockHitResult.miss(hitPos, direction, above)));
            } else {
                boolean inside = BlockHitResultProxy.INSTANCE.isInside(blockHitResult);
                return super.useOnBlock(new UseOnContext(player, hand, new BlockHitResult(hitPos, direction, above, inside)));
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Error handling use", e);
            return InteractionResult.FAIL;
        }
    }

    private static class Factory implements ItemBehaviorFactory<LiquidCollisionBlockItemBehavior> {
        @Override
        public LiquidCollisionBlockItemBehavior create(Pack pack, Path path, String node, Key key, Map<String, Object> arguments) {
            Object id = arguments.get("block");
            if (id == null) {
                throw new LocalizedResourceConfigException("warning.config.item.behavior.liquid_collision.missing_block", new IllegalArgumentException("Missing required parameter 'block' for liquid_collision_block_item behavior"));
            }
            int offset = ResourceConfigUtils.getAsInt(arguments.getOrDefault("y-offset", 1), "y-offset");
            if (id instanceof Map<?, ?> map) {
                addPendingSection(pack, path, node, key, map);
                return new LiquidCollisionBlockItemBehavior(key, offset);
            } else {
                return new LiquidCollisionBlockItemBehavior(Key.of(id.toString()), offset);
            }
        }
    }
}
