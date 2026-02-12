package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MItems;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.MirrorUtils;
import net.momirealms.craftengine.bukkit.util.RotationUtils;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehavior;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

public abstract class BukkitBlockBehavior extends BlockBehavior {
    private static final Map<String, BiConsumer<@NotNull BukkitBlockBehavior, Property<?>>> HARD_CODED_PROPERTY_DATA = new HashMap<>();
    static {
        HARD_CODED_PROPERTY_DATA.put("axis", (behavior, property) -> {
            @SuppressWarnings("unchecked")
            Property<Direction.Axis> axisProperty = (Property<Direction.Axis>) property;
            behavior.rotateFunction = (thisBlock, blockState, rotation) -> {
                Direction.Axis axis = blockState.get(axisProperty);
                return switch (rotation) {
                    case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (axis) {
                        case X -> blockState.with(axisProperty, Direction.Axis.Z).customBlockState().literalObject();
                        case Z -> blockState.with(axisProperty, Direction.Axis.X).customBlockState().literalObject();
                        default -> blockState.customBlockState().literalObject();
                    };
                    default -> blockState.customBlockState().literalObject();
                };
            };
        });
        HARD_CODED_PROPERTY_DATA.put("facing", (behavior, property) -> {
            if (property.valueClass() == HorizontalDirection.class) {
                @SuppressWarnings("unchecked")
                Property<HorizontalDirection> directionProperty = (Property<HorizontalDirection>) property;
                behavior.rotateFunction = (thisBlock, blockState, rotation) ->
                        blockState.with(directionProperty, rotation.rotate(blockState.get(directionProperty).toDirection()).toHorizontalDirection())
                                .customBlockState().literalObject();
                behavior.mirrorFunction = (thisBlock, blockState, mirror) -> {
                    Rotation rotation = mirror.getRotation(blockState.get(directionProperty).toDirection());
                    return behavior.rotateFunction.rotate(thisBlock, blockState, rotation);
                };
            } else if (property.valueClass() == Direction.class) {
                @SuppressWarnings("unchecked")
                Property<Direction> directionProperty = (Property<Direction>) property;
                behavior.rotateFunction = (thisBlock, blockState, rotation) ->
                        blockState.with(directionProperty, rotation.rotate(blockState.get(directionProperty)))
                                .customBlockState().literalObject();
                behavior.mirrorFunction = (thisBlock, blockState, mirror) -> {
                    Rotation rotation = mirror.getRotation(blockState.get(directionProperty));
                    return behavior.rotateFunction.rotate(thisBlock, blockState, rotation);
                };
            }
        });
        HARD_CODED_PROPERTY_DATA.put("facing_clockwise", (behavior, property) -> {
            if (property.valueClass() == HorizontalDirection.class) {
                @SuppressWarnings("unchecked")
                Property<HorizontalDirection> directionProperty = (Property<HorizontalDirection>) property;
                behavior.rotateFunction = (thisBlock, blockState, rotation) ->
                        blockState.with(directionProperty, rotation.rotate(blockState.get(directionProperty).toDirection()).toHorizontalDirection())
                                .customBlockState().literalObject();
                behavior.mirrorFunction = (thisBlock, blockState, mirror) -> {
                    Rotation rotation = mirror.getRotation(blockState.get(directionProperty).toDirection());
                    return behavior.rotateFunction.rotate(thisBlock, blockState, rotation);
                };
            }
        });
    }

    @Nullable
    private MirrorFunction mirrorFunction;
    @Nullable
    private RotateFunction rotateFunction;
    @Nullable
    protected final Property<Boolean> waterloggedProperty;

    @SuppressWarnings("unchecked")
    public BukkitBlockBehavior(CustomBlock customBlock) {
        super(customBlock);
        for (Property<?> property : customBlock.properties()) {
            Optional.ofNullable(HARD_CODED_PROPERTY_DATA.get(property.name())).ifPresent(c -> c.accept(this, property));
        }
        this.waterloggedProperty = (Property<Boolean>) customBlock.getProperty("waterlogged");
    }

    @Override
    public Object mirror(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        if (this.mirrorFunction != null) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
            if (optionalCustomState.isEmpty()) return args[0];
            return this.mirrorFunction.mirror(thisBlock, optionalCustomState.get(), MirrorUtils.fromNMSMirror(args[1]));
        }
        return super.mirror(thisBlock, args, superMethod);
    }

    @Override
    public Object rotate(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        if (this.rotateFunction != null) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
            if (optionalCustomState.isEmpty()) return args[0];
            return this.rotateFunction.rotate(thisBlock, optionalCustomState.get(), RotationUtils.fromNMSRotation(args[1]));
        }
        return super.rotate(thisBlock, args, superMethod);
    }

    @FunctionalInterface
    interface MirrorFunction {

        Object mirror(Object thisBlock, ImmutableBlockState state, Mirror mirror) throws Exception;
    }

    @FunctionalInterface
    interface RotateFunction {

        Object rotate(Object thisBlock, ImmutableBlockState state, Rotation rotation) throws Exception;
    }

    private static final int pickupBlock$world = VersionHelper.isOrAbove1_20_2() ? 1 : 0;
    private static final int pickupBlock$pos = VersionHelper.isOrAbove1_20_2() ? 2 : 1;
    private static final int pickupBlock$blockState = VersionHelper.isOrAbove1_20_2() ? 3 : 2;

    @Override
    public Object pickupBlock(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        if (this.waterloggedProperty == null) return CoreReflections.instance$ItemStack$EMPTY;
        Object blockState = args[pickupBlock$blockState];
        Object world = args[pickupBlock$world];
        Object pos = args[pickupBlock$pos];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return CoreReflections.instance$ItemStack$EMPTY;
        ImmutableBlockState immutableBlockState = optionalCustomState.get();
        if (immutableBlockState.get(this.waterloggedProperty)) {
            FastNMS.INSTANCE.method$LevelWriter$setBlock(world, pos, immutableBlockState.with(this.waterloggedProperty, false).customBlockState().literalObject(), 3);
            return FastNMS.INSTANCE.constructor$ItemStack(MItems.WATER_BUCKET, 1);
        }
        return CoreReflections.instance$ItemStack$EMPTY;
    }

    @Override
    public boolean placeLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        if (this.waterloggedProperty == null) return false;
        Object blockState = args[2];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return false;
        ImmutableBlockState immutableBlockState = optionalCustomState.get();
        Object fluidType = FastNMS.INSTANCE.method$FluidState$getType(args[3]);
        if (!immutableBlockState.get(this.waterloggedProperty) && fluidType == MFluids.WATER) {
            FastNMS.INSTANCE.method$LevelWriter$setBlock(args[0], args[1], immutableBlockState.with(this.waterloggedProperty, true).customBlockState().literalObject(), 3);
            FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleFluidTick(args[0], args[1], fluidType, 5);
            return true;
        }
        return false;
    }

    private static final int canPlaceLiquid$liquid = VersionHelper.isOrAbove1_20_2() ? 4 : 3;

    @Override
    public boolean canPlaceLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        if (this.waterloggedProperty == null) return false;
        return args[canPlaceLiquid$liquid] == MFluids.WATER;
    }

    protected static final int updateShape$level = VersionHelper.isOrAbove1_21_2() ? 1 : 3;
    protected static final int updateShape$blockPos = VersionHelper.isOrAbove1_21_2() ? 3 : 4;
    protected static final int updateShape$neighborState = VersionHelper.isOrAbove1_21_2() ? 6 : 2;
    protected static final int updateShape$direction = VersionHelper.isOrAbove1_21_2() ? 4 : 1;

    protected static final int isPathFindable$type = VersionHelper.isOrAbove1_20_5() ? 1 : 3;

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
        if (optionalCustomState.isEmpty()) return false;
        BlockStateWrapper vanillaState = optionalCustomState.get().visualBlockState();
        if (vanillaState == null) return false;
        return FastNMS.INSTANCE.method$BlockStateBase$isPathFindable(vanillaState.literalObject(), VersionHelper.isOrAbove1_20_5() ? null : args[1], VersionHelper.isOrAbove1_20_5() ? null : args[2], args[isPathFindable$type]);
    }
}
