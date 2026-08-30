package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.block.behavior.LiquidSolidifiableBlock;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.FallableBlock;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.event.CraftEventFactoryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MutableBlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.item.FallingBlockEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ClipContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.BlockHitResultProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.Nullable;

public final class LiquidSolidification {
    private LiquidSolidification() {
    }

    public static @Nullable ImmutableBlockState solidifiedStateAt(
            LiquidSolidifiableBlock solidifiable,
            ImmutableBlockState sourceState,
            Object level,
            Object pos,
            Object replacedState
    ) {
        if (canSolidifyWith(solidifiable, replacedState)) {
            return solidifiable.solidifiedState(sourceState);
        }
        return touchingLiquid(solidifiable, sourceState, level, pos);
    }

    public static @Nullable ImmutableBlockState touchingLiquid(
            LiquidSolidifiableBlock solidifiable,
            ImmutableBlockState sourceState,
            Object level,
            Object pos
    ) {
        Object mutablePos = BlockPosProxy.INSTANCE.mutable(pos);
        for (int i = 0; i < Direction.values().length; i++) {
            Object direction = DirectionProxy.VALUES[i];
            Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, mutablePos);
            if (direction != DirectionProxy.DOWN || canSolidifyWith(solidifiable, blockState)) {
                MutableBlockPosProxy.INSTANCE.setWithOffset(mutablePos, pos, direction);
                blockState = BlockGetterProxy.INSTANCE.getBlockState(level, mutablePos);
                if (canSolidifyWith(solidifiable, blockState) && !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(
                        blockState,
                        level,
                        pos,
                        DirectionProxy.INSTANCE.getOpposite(direction),
                        SupportTypeProxy.FULL
                )) {
                    return solidifiable.solidifiedState(sourceState);
                }
            }
        }
        return null;
    }

    public static void solidifyOnLand(LiquidSolidifiableBlock solidifiable, Object[] args) {
        ImmutableBlockState sourceState = BlockStateUtils.getOptionalCustomBlockState(args[2]).orElse(null);
        if (sourceState == null) {
            return;
        }
        ImmutableBlockState targetState = solidifiedStateAt(solidifiable, sourceState, args[0], args[1], args[3]);
        if (targetState != null) {
            CraftEventFactoryProxy.INSTANCE.handleBlockFormEvent(
                    args[0],
                    args[1],
                    targetState.customBlockState().minecraftState(),
                    UpdateFlags.UPDATE_ALL
            );
        }
    }

    public static boolean solidifyFallingBlock(
            Object fallingBlockEntity,
            Object from,
            LiquidSolidifiableBlock solidifiable
    ) {
        if (EntityProxy.INSTANCE.isRemoved(fallingBlockEntity)) {
            return false;
        }
        Object level = EntityProxy.INSTANCE.getLevel(fallingBlockEntity);
        Object to = EntityProxy.INSTANCE.getPosition(fallingBlockEntity);
        Object contactPos = blockPosition(to);
        Object replacedState = BlockGetterProxy.INSTANCE.getBlockState(level, contactPos);
        if (!canSolidifyWith(solidifiable, replacedState)) {
            Object movement = EntityProxy.INSTANCE.getDeltaMovement(fallingBlockEntity);
            double dx = Vec3Proxy.INSTANCE.getX(movement);
            double dy = Vec3Proxy.INSTANCE.getY(movement);
            double dz = Vec3Proxy.INSTANCE.getZ(movement);
            if (dx * dx + dy * dy + dz * dz <= 1.0) {
                return false;
            }
            Object clipContext = ClipContextProxy.INSTANCE.newInstance(
                    from,
                    to,
                    ClipContextProxy.BlockProxy.COLLIDER,
                    ClipContextProxy.FluidProxy.SOURCE_ONLY,
                    fallingBlockEntity
            );
            Object hitResult = BlockGetterProxy.INSTANCE.clip(level, clipContext);
            if (BlockHitResultProxy.INSTANCE.isMiss(hitResult)) {
                return false;
            }
            contactPos = BlockHitResultProxy.INSTANCE.getBlockPos(hitResult);
            replacedState = BlockGetterProxy.INSTANCE.getBlockState(level, contactPos);
            if (!canSolidifyWith(solidifiable, replacedState)) {
                return false;
            }
        }

        Object fallingState = FallingBlockEntityProxy.INSTANCE.getBlockState(fallingBlockEntity);
        ImmutableBlockState sourceState = BlockStateUtils.getOptionalCustomBlockState(fallingState).orElse(null);
        if (sourceState == null) {
            return false;
        }
        ImmutableBlockState targetState = solidifiable.solidifiedState(sourceState);
        if (targetState == null
                || !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isReplaceable(replacedState)
                || !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(fallingState, level, contactPos)) {
            return false;
        }

        if (!CraftEventFactoryProxy.INSTANCE.callEntityChangeBlockEvent(fallingBlockEntity, contactPos, fallingState)) {
            EntityProxy.INSTANCE.discard(fallingBlockEntity);
            return true;
        }
        if (!LevelWriterProxy.INSTANCE.setBlock(level, contactPos, fallingState, UpdateFlags.UPDATE_ALL)) {
            return false;
        }

        EntityProxy.INSTANCE.discard(fallingBlockEntity);
        CraftEventFactoryProxy.INSTANCE.handleBlockFormEvent(
                level,
                contactPos,
                targetState.customBlockState().minecraftState(),
                UpdateFlags.UPDATE_ALL
        );
        if (sourceState.behavior() instanceof FallableBlock fallable) {
            Object thisBlock = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(fallingState);
            fallable.onLand(thisBlock, new Object[]{level, contactPos, fallingState, replacedState, fallingBlockEntity});
        }
        return true;
    }

    private static boolean canSolidifyWith(LiquidSolidifiableBlock solidifiable, Object blockState) {
        Object fluidState = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(blockState);
        return fluidState != null && solidifiable.canSolidifyWith(fluidState);
    }

    private static Object blockPosition(Object position) {
        return BlockPosProxy.INSTANCE.newInstance(
                (int) Math.floor(Vec3Proxy.INSTANCE.getX(position)),
                (int) Math.floor(Vec3Proxy.INSTANCE.getY(position)),
                (int) Math.floor(Vec3Proxy.INSTANCE.getZ(position))
        );
    }
}
