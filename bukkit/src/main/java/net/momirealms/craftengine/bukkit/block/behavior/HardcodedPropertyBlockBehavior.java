package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.MirrorUtils;
import net.momirealms.craftengine.bukkit.util.RotationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Mirror;
import net.momirealms.craftengine.core.util.Rotation;
import net.momirealms.craftengine.core.util.SegmentedAngle;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@SuppressWarnings("unchecked")
public final class HardcodedPropertyBlockBehavior extends BukkitBlockBehavior {
    public static final Map<String, BiFunction<BlockDefinition, Property<?>, HardcodedPropertyBlockBehavior>> HARD_CODED_PROPERTY_DATA = new HashMap<>();

    static {
        HARD_CODED_PROPERTY_DATA.put("axis", (block, property) -> {
            Property<Direction.Axis> axisProperty = (Property<Direction.Axis>) property;
            return new HardcodedPropertyBlockBehavior(block, null, (blockState, rotation) -> {
                Direction.Axis axis = blockState.get(axisProperty);
                return switch (rotation) {
                    case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (axis) {
                        case X -> blockState.with(axisProperty, Direction.Axis.Z).customBlockState().minecraftState();
                        case Z -> blockState.with(axisProperty, Direction.Axis.X).customBlockState().minecraftState();
                        default -> blockState.customBlockState().minecraftState();
                    };
                    default -> blockState.customBlockState().minecraftState();
                };
            }, (context, state) -> state.with(axisProperty, context.getClickedFace().axis()));
        });
        HARD_CODED_PROPERTY_DATA.put("facing", (block, property) -> {
            if (property.valueClass() == Direction.class) {
                Property<Direction> directionProperty = (Property<Direction>) property;
                return new HardcodedPropertyBlockBehavior(
                        block,
                        (blockState, mirror) -> {
                            Rotation rotation = mirror.getRotation(blockState.get(directionProperty));
                            return blockState.with(directionProperty, rotation.rotate(blockState.get(directionProperty)))
                                    .customBlockState().minecraftState();
                        },
                        (blockState, rotation) ->
                                blockState.with(directionProperty, rotation.rotate(blockState.get(directionProperty)))
                                        .customBlockState().minecraftState(),
                        (context, state) -> {
                            List<?> values = property.possibleValues();
                            if (values.size() == 6) {
                                return state.with(directionProperty, context.getNearestLookingDirection().opposite());
                            } else if (values.size() == 4 && !values.contains(Direction.UP) && !values.contains(Direction.DOWN)) {
                                return state.with(directionProperty, context.getHorizontalDirection().opposite());
                            } else if (values.size() == 2 && values.contains(Direction.UP) && values.contains(Direction.DOWN)) {
                                return state.with(directionProperty, context.getVerticalLookingDirection().opposite());
                            }
                            return state;
                        }
                );
            }
            return null;
        });
        HARD_CODED_PROPERTY_DATA.put("facing_clockwise", (block, property) -> {
            if (property.valueClass() == Direction.class) {
                @SuppressWarnings("unchecked")
                Property<Direction> directionProperty = (Property<Direction>) property;
                return new HardcodedPropertyBlockBehavior(
                        block,
                        (blockState, mirror) -> {
                            Rotation rotation = mirror.getRotation(blockState.get(directionProperty));
                            return blockState.with(directionProperty, rotation.rotate(blockState.get(directionProperty)))
                                    .customBlockState().minecraftState();
                        },
                        (blockState, rotation) ->
                                blockState.with(directionProperty, rotation.rotate(blockState.get(directionProperty)))
                                        .customBlockState().minecraftState(),
                        (context, state) -> {
                            List<?> values = property.possibleValues();
                            if (values.size() == 6) {
                                return state.with(directionProperty, context.getNearestLookingDirection().clockWise());
                            } else if (values.size() == 4 && !values.contains(Direction.UP) && !values.contains(Direction.DOWN)) {
                                return state.with(directionProperty, context.getHorizontalDirection().clockWise());
                            }
                            return state;
                        }
                );
            }
            return null;
        });
        HARD_CODED_PROPERTY_DATA.put("rotation", (block, property) -> {
            if (property.valueClass() == Integer.class) {
                IntegerProperty rotationProperty = (IntegerProperty) property;
                if (rotationProperty.min == 0 && rotationProperty.max > 0) {
                    int segments = rotationProperty.max + 1;
                    return new HardcodedPropertyBlockBehavior(
                            block,
                            (blockState, mirror) -> {
                                int currentRotation = blockState.get(rotationProperty);
                                int mirrored = mirror.mirror(currentRotation, segments);
                                return blockState.with(rotationProperty, mirrored).customBlockState().minecraftState();
                            },
                            (blockState, rotation) -> {
                                int currentRotation = blockState.get(rotationProperty);
                                int rotated = rotation.rotate(currentRotation, segments);
                                return blockState.with(rotationProperty, rotated).customBlockState().minecraftState();
                            },
                            (context, state) -> {
                                float rotation = context.getRotation();
                                SegmentedAngle segmentedAngle = new SegmentedAngle(rotationProperty.max + 1);
                                return state.with(rotationProperty, segmentedAngle.fromDegrees(rotation + 180));
                            }
                    );
                }
            }
            return null;
        });
    }

    @Nullable
    private final MirrorFunction mirrorFunction;
    @Nullable
    private final RotateFunction rotateFunction;
    @Nullable
    private final UpdateFunction updateFunction;

    public HardcodedPropertyBlockBehavior(@NotNull BlockDefinition blockDefinition,
                                          @Nullable MirrorFunction mirrorFunction,
                                          @Nullable RotateFunction rotateFunction,
                                          @Nullable UpdateFunction updateFunction) {
        super(blockDefinition);
        this.mirrorFunction = mirrorFunction;
        this.rotateFunction = rotateFunction;
        this.updateFunction = updateFunction;
    }

    @Override
    public Object mirror(Object thisBlock, Object[] args) {
        if (this.mirrorFunction != null) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
            if (optionalCustomState.isEmpty()) return args[0];
            return this.mirrorFunction.mirror(optionalCustomState.get(), MirrorUtils.fromNMSMirror(args[1]));
        }
        return super.mirror(thisBlock, args);
    }

    @Override
    public Object rotate(Object thisBlock, Object[] args) {
        if (this.rotateFunction != null) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
            if (optionalCustomState.isEmpty()) return args[0];
            return this.rotateFunction.rotate(optionalCustomState.get(), RotationUtils.fromNMSRotation(args[1]));
        }
        return super.rotate(thisBlock, args);
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        if (this.updateFunction != null) {
            return this.updateFunction.updateStateForPlacement(context, state);
        }
        return super.updateStateForPlacement(context, state);
    }

    @FunctionalInterface
    public interface MirrorFunction {

        Object mirror(ImmutableBlockState state, Mirror mirror);
    }

    @FunctionalInterface
    public interface RotateFunction {

        Object rotate(ImmutableBlockState state, Rotation rotation);
    }

    @FunctionalInterface
    public interface UpdateFunction {

        ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state);
    }
}
