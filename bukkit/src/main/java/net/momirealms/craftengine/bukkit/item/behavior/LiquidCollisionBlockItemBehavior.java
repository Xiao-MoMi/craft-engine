package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockHitResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ClipContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
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
            Object blockHitResult = ItemProxy.INSTANCE.getPlayerPOVHitResult(world.minecraftWorld(), player.serverPlayer(), ClipContextProxy.FluidProxy.SOURCE_ONLY);
            Object blockPos = BlockHitResultProxy.INSTANCE.getBlockPos(blockHitResult);
            BlockPos above = new BlockPos(Vec3iProxy.INSTANCE.getX(blockPos), Vec3iProxy.INSTANCE.getY(blockPos) + offsetY, Vec3iProxy.INSTANCE.getZ(blockPos));
            Direction direction = DirectionUtils.fromNMSDirection(BlockHitResultProxy.INSTANCE.getDirection(blockHitResult));
            boolean miss = BlockHitResultProxy.INSTANCE.isMiss(blockHitResult);
            Vec3d hitPos = LocationUtils.fromVec(HitResultProxy.INSTANCE.getLocation(blockHitResult));
            Object fluidType = FluidStateProxy.INSTANCE.getType(BlockGetterProxy.INSTANCE.getFluidState(world.minecraftWorld(), blockPos));
            if (fluidType != FluidsProxy.WATER && fluidType != FluidsProxy.LAVA) {
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
        private static final String[] Y_OFFSET = new String[]{"y_offset", "y-offset"};

        @Override
        public LiquidCollisionBlockItemBehavior create(Pack pack, Path path, Key key, ConfigSection section) {
            int offset = section.getInt(Y_OFFSET, 1);
            ConfigValue blockValue = section.getNonNullValue("block", ConfigConstants.ARGUMENT_SECTION);
            if (blockValue.is(Map.class)) {
                BukkitBlockManager.instance().blockParser().addPendingConfigSection(new PendingConfigSection(pack, path, key, blockValue.getAsSection()));
                return new LiquidCollisionBlockItemBehavior(key, offset);
            } else {
                return new LiquidCollisionBlockItemBehavior(blockValue.getAsIdentifier(), offset);
            }
        }
    }
}
