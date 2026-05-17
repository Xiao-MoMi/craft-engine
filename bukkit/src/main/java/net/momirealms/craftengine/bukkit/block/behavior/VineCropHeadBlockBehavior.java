package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.BonemealableBlock;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.block.property.BooleanProperty;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.util.random.ThreadLocalRandomSource;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.event.CraftEventFactoryProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelHeightAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class VineCropHeadBlockBehavior extends AbstractCanSurviveBlockBehavior implements BonemealableBlock, RandomTickBlock {
    public static final BlockBehaviorFactory<VineCropHeadBlockBehavior> FACTORY = new Factory();
    public final int maxHeight;
    public final IntegerProperty ageProperty;
    @Nullable
    public final Property<Boolean> berriesProperty;
    public final float growSpeed;
    public final int baseGrowth;
    public final float extraGrowChance;
    public final boolean direction;
    public final Key body;
    public final boolean isBoneMealGrow;
    @Nullable
    public final NumberProvider boneMealGrowBlocks;

    private VineCropHeadBlockBehavior(BlockDefinition blockDefinition,
                                      IntegerProperty ageProperty,
                                      @Nullable Property<Boolean> berriesProperty,
                                      int maxHeight,
                                      float growSpeed,
                                      boolean direction,
                                      Key body,
                                      int dealy,
                                      boolean isBoneMealGrow,
                                      @Nullable NumberProvider boneMealGrowBlocks
    ) {
        super(blockDefinition, dealy);
        this.ageProperty = ageProperty;
        this.berriesProperty = berriesProperty;
        this.maxHeight = maxHeight;
        this.growSpeed = growSpeed;
        this.baseGrowth = (int) growSpeed;
        this.extraGrowChance = growSpeed - baseGrowth;
        this.direction = direction;
        this.body = body;
        this.isBoneMealGrow = isBoneMealGrow;
        this.boneMealGrowBlocks = boneMealGrowBlocks;
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return true;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        Optional<ImmutableBlockState> optionalCurrentState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCurrentState.isEmpty()) return;
        ImmutableBlockState currentState = optionalCurrentState.get();

        // above block is empty
        Object targetPos = this.direction ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos);
        if (BlockGetterProxy.INSTANCE.getBlockState(level, targetPos) == BlocksProxy.AIR$defaultState) {
            int currentHeight = this.getCurrentHeight(level, blockPos);
            if (currentHeight < this.maxHeight) {
                // 计算更新之后的 Age
                int age = currentState.get(ageProperty) + baseGrowth;
                if (age < this.ageProperty.max && this.extraGrowChance > 0 && RandomUtils.generateRandomFloat(0, 1) < this.extraGrowChance) {
                    age++;
                }
                // 检查是否需要生长
                if (age >= this.ageProperty.max) {
                    Object nextPos = this.direction ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos);
                    boolean success = VersionHelper.isOrAbove1_21_5
                            ? CraftEventFactoryProxy.INSTANCE.handleBlockGrowEvent(level, nextPos, super.blockDefinition.defaultState().customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL)
                            : CraftEventFactoryProxy.INSTANCE.handleBlockGrowEvent(level, nextPos, super.blockDefinition.defaultState().customBlockState().minecraftState());
                    if (success) {
                        boolean hasBerries = this.berriesProperty != null ? currentState.get(berriesProperty) : false;
                        Object convertedState = this.convertBody(hasBerries);
                        if (convertedState != null) {
                            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, convertedState, UpdateFlags.UPDATE_NONE);
                        }
                    }
                } else {
                    LevelWriterProxy.INSTANCE.setBlock(level, blockPos, currentState.with(this.ageProperty, age).customBlockState().minecraftState(), UpdateFlags.UPDATE_NONE);
                }
            }
        }
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object state = args[0];
        Direction direction = DirectionUtils.fromNMSDirection(args[updateShape$direction]);
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty() || direction != (this.direction ? Direction.UP : Direction.DOWN)) {
            return super.updateShape(thisBlock, args);
        }

        ImmutableBlockState thisBlockState = optionalCustomState.get();
        Key thisBlockId = thisBlockState.owner().value().id();
        Object neighborState = args[updateShape$neighborState];
        Optional<ImmutableBlockState> optionalCustomNeighborState = BlockStateUtils.getOptionalCustomBlockState(neighborState);
        Key neighborBlockId = optionalCustomNeighborState.isPresent()
                ? optionalCustomNeighborState.get().owner().value().id()
                : BlockStateUtils.getBlockOwnerIdFromState(neighborState);
        if (thisBlockId.equals(neighborBlockId)) {
            boolean berries = berriesProperty != null ? thisBlockState.get(berriesProperty) : false;
            Object bodyBlock = this.convertBody(berries);
            if (bodyBlock != null) return bodyBlock;
        }

        return super.updateShape(thisBlock, args);
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object level, Object blockPos) {
        Object attachedToPos = this.direction ? LocationUtils.below(blockPos) : LocationUtils.above(blockPos);
        Object attachedToState = BlockGetterProxy.INSTANCE.getBlockState(level, attachedToPos);

        boolean faceSturdy = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(
                attachedToState, level, attachedToPos, DirectionUtils.toNMSDirection(this.direction ? Direction.UP : Direction.DOWN), SupportTypeProxy.FULL
        );
        if (faceSturdy) return true;

        Optional<ImmutableBlockState> optionalTargetBlockState = BlockStateUtils.getOptionalCustomBlockState(attachedToState);
        Key targetBlockId = optionalTargetBlockState.isPresent()
                ? optionalTargetBlockState.get().owner().value().id()
                : BlockStateUtils.getBlockOwnerIdFromState(attachedToState);
        if (this.body.equals(targetBlockId)) return true;

        Optional<ImmutableBlockState> optionalBlockState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalBlockState.isEmpty()) return false;
        Key thisId = optionalBlockState.get().owner().value().id();
        return thisId.equals(targetBlockId);
    }

    @Override
    public boolean isValidBonemealTarget(Object thisBlock, Object[] args) {
        Object level = args[0];
        Object blockPos = args[1];
        Object state = args[2];

        if (!isBoneMealGrow) {
            if (this.berriesProperty == null) return false;
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
            if (optionalCustomState.isEmpty()) return false;
            ImmutableBlockState customState = optionalCustomState.get();
            return !customState.get(berriesProperty);
        }
        else {
            Optional<BlockDefinition> optionalBodyBlock = BukkitBlockManager.instance().blockById(this.body);
            if (optionalBodyBlock.isEmpty()) return false;
            // 检查生长目标位置是否限高且为空气.
            Object targetPos = this.direction ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos);
            if (LevelHeightAccessorProxy.INSTANCE.isOutsideBuildHeight(level, targetPos)) return false;
            if (BlockGetterProxy.INSTANCE.getBlockState(level, targetPos) != BlocksProxy.AIR$defaultState) return false;
            // 检查生长高度上限
            int currentHeight = this.getCurrentHeight(level, blockPos);
            if (this.maxHeight != -1 && currentHeight >= this.maxHeight) return false;
            return true;
        }
    }

    @Override
    public boolean isBonemealSuccess(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public void performBonemeal(Object thisBlock, Object[] args) {
        Object level = args[0];
        Object blockPos = args[2];
        Object state = args[3];

        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty()) return;
        ImmutableBlockState customState = optionalCustomState.get();

        if (!isBoneMealGrow) {
            LevelWriterProxy.INSTANCE.setBlock(
                    level, blockPos,
                    customState.with(this.berriesProperty, true).customBlockState().minecraftState(),
                    UpdateFlags.UPDATE_ALL
            );
        }
        else {
            BlockPos currentPos = LocationUtils.fromBlockPos(blockPos);
            int currentHeight = this.getCurrentHeight(level, blockPos);
            int blocksToGrowable = Math.min(maxHeight - currentHeight, this.getBlocksToGrowWhenBoneMealed());
            int headAge = Math.min(customState.get(ageProperty) + 1, ageProperty.max);

            for (int i = 1; i <= blocksToGrowable; i++) {
                Object nextPos = LocationUtils.toBlockPos(currentPos.x(), this.direction ? currentPos.y() + i : currentPos.y() - i, currentPos.z());
                if (BlockGetterProxy.INSTANCE.getBlockState(level, nextPos) == BlocksProxy.AIR$defaultState
                    && !LevelHeightAccessorProxy.INSTANCE.isOutsideBuildHeight(level, nextPos)
                ) {
                    LevelWriterProxy.INSTANCE.setBlock(
                            level, nextPos,
                            customState.with(ageProperty, headAge).customBlockState().minecraftState(),
                            UpdateFlags.UPDATE_ALL
                    );
                } else {
                    break;
                }
            }
        }
    }

    // 计算作物当前高度
    private int getCurrentHeight(Object level, Object blockPos) {
        int currentHeight = 1;
        BlockPos currentPos = LocationUtils.fromBlockPos(blockPos);
        for (;;) {
            Object nextPos = LocationUtils.toBlockPos(currentPos.x(), this.direction ? currentPos.y() - currentHeight : currentPos.y() + currentHeight, currentPos.z());
            Object nextState = BlockGetterProxy.INSTANCE.getBlockState(level, nextPos);
            Optional<ImmutableBlockState> optionalBelowCustomState = BlockStateUtils.getOptionalCustomBlockState(nextState);
            if (optionalBelowCustomState.isPresent() && optionalBelowCustomState.get().owner().value().id().equals(this.body)) {
                currentHeight++;
            } else {
                break;
            }
        }
        return currentHeight;
    }

    // 计算骨粉催熟后生长高度
    private int getBlocksToGrowWhenBoneMealed() {
        if (this.boneMealGrowBlocks != null) {
            this.boneMealGrowBlocks.getInt(ThreadLocalRandomSource.INSTANCE);
        }
        return 1;
    }

    // 转换成 Body 方块
    private Object convertBody(boolean berries) {
        Optional<BlockDefinition> optionalBodyBlock = BukkitBlockManager.instance().blockById(this.body);
        if (optionalBodyBlock.isPresent()) {
            BlockDefinition headBlock = optionalBodyBlock.get();
            BooleanProperty berriesProperty = (BooleanProperty) headBlock.getProperty("berries");
            return Optional.of(headBlock.defaultState())
                    .map(it -> berriesProperty != null ? it.with(berriesProperty, berries) : it)
                    .map(it -> it.customBlockState().minecraftState())
                    .orElse(null);
        }
        return null;
    }

    private static class Factory implements BlockBehaviorFactory<VineCropHeadBlockBehavior> {
        private static final String[] MAX_HEIGHT = new String[] {"max_height", "max-height"};
        private static final String[] GROW_SPEED = new String[] {"grow_speed", "grow-speed"};
        private static final String[] BONE_MEAL = new String[] {"bone_meal", "bone-meal"};
        private static final String[] GROW_BLOCKS = new String[] {"grow_blocks", "grow-blocks"};

        @Override
        public VineCropHeadBlockBehavior create(BlockDefinition block, ConfigSection section) {
            boolean boneMealBehavior = false; // false 代表 berries, true 代表 grow
            NumberProvider growBlocks = null;
            ConfigSection boneMealSection = section.getSection(BONE_MEAL);
            if (boneMealSection != null) {
                boneMealBehavior = boneMealSection.getString("behavior", "berries").equalsIgnoreCase("grow");
                if (boneMealBehavior) {
                    growBlocks = boneMealSection.getNumber(GROW_BLOCKS, ConfigConstants.CONSTANT_ONE);
                }
            }

            return new VineCropHeadBlockBehavior(
                    block,
                    (IntegerProperty) BlockBehaviorFactory.getProperty(section.path(), block, "age", Integer.class),
                    BlockBehaviorFactory.getOptionalProperty(block, "berries", Boolean.class),
                    section.getInt(MAX_HEIGHT, -1),
                    section.getFloat(GROW_SPEED, 1f),
                    section.getString("direction", "up").equalsIgnoreCase("up"),
                    section.getNonNullIdentifier("body"),
                    section.getInt("delay", 1),
                    boneMealBehavior,
                    growBlocks
            );
        }
    }
}
