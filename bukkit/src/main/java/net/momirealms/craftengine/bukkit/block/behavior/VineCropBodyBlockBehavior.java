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
import net.momirealms.craftengine.core.block.property.BooleanProperty;
import net.momirealms.craftengine.core.block.property.IntegerProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelHeightAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BonemealableBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockStateProxy;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class VineCropBodyBlockBehavior extends AbstractCanSurviveBlockBehavior implements BonemealableBlock {
    public static final BlockBehaviorFactory<VineCropBodyBlockBehavior> FACTORY = new Factory();
    @Nullable
    public final Property<Boolean> berriesProperty;
    public final Key head;
    public final boolean direction;
    public final boolean isBoneMealGrow;
    @Nullable
    public final NumberProvider boneMealGrowBlocks;

    private VineCropBodyBlockBehavior(BlockDefinition blockDefinition,
                                      @Nullable Property<Boolean> berriesProperty,
                                      boolean direction,
                                      Key head,
                                      int delay,
                                      boolean isBoneMealGrow,
                                      @Nullable NumberProvider boneMealGrowBlocks
    ) {
        super(blockDefinition, delay);
        this.berriesProperty = berriesProperty;
        this.direction = direction;
        this.head = head;
        this.isBoneMealGrow = isBoneMealGrow;
        this.boneMealGrowBlocks = boneMealGrowBlocks;
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
        Object neighborState = args[updateShape$neighborState];
        Optional<ImmutableBlockState> optionalCustomNeighborState = BlockStateUtils.getOptionalCustomBlockState(neighborState);
        Key neighborBlockId = optionalCustomNeighborState.isPresent()
                ? optionalCustomNeighborState.get().owner().value().id()
                : BlockStateUtils.getBlockOwnerIdFromState(neighborState);
        if (!head.equals(neighborBlockId) && !neighborBlockId.equals(thisBlockState.owner().value().id())) {
            boolean berries = berriesProperty != null ? thisBlockState.get(berriesProperty) : false;
            Object headBlock = this.convertHead(berries);
            if (headBlock != null) return headBlock;
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

        Optional<ImmutableBlockState> optionalStackBlockState = BlockStateUtils.getOptionalCustomBlockState(attachedToState);
        if (optionalStackBlockState.isEmpty()) return false;
        Optional<ImmutableBlockState> optionalBlockState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalBlockState.isEmpty()) return false;

        Key stackId = optionalStackBlockState.get().owner().value().id();
        Key thisId = optionalBlockState.get().owner().value().id();
        return thisId.equals(stackId);
    }

    private Object convertHead(boolean berries) {
        Optional<BlockDefinition> optionalHeadBlock = BukkitBlockManager.instance().blockById(this.head);
        if (optionalHeadBlock.isPresent()) {
            BlockDefinition headBlock = optionalHeadBlock.get();
            BooleanProperty berriesProperty = (BooleanProperty) headBlock.getProperty("berries");
            IntegerProperty ageProperty = (IntegerProperty) headBlock.getProperty("age");
            return Optional.of(headBlock.defaultState())
                    .map(it -> berriesProperty != null ? it.with(berriesProperty, berries) : it)
                    .map(it -> ageProperty != null ? it.with(ageProperty, ageProperty.min) : it)
                    .map(it -> it.customBlockState().minecraftState())
                    .orElse(null);
        }
        return null;
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
            Optional<BlockDefinition> optionalBodyBlock = BukkitBlockManager.instance().blockById(this.head);
            if (optionalBodyBlock.isEmpty()) return false;

            Object headBlockPos = this.getHeadBlockPos(level, blockPos, state);
            if (headBlockPos == null) return false;
            Object headBlockState = LevelReaderProxy.INSTANCE.getBlockState(level, headBlockPos);
            Object headBlock = BlockStateProxy.INSTANCE.getBlock(headBlockState);
            if (BonemealableBlockProxy.CLASS.isInstance(headBlock)) {
                return BonemealableBlockProxy.INSTANCE.isValidBonemealTarget(headBlock, level, headBlockPos, headBlockState);
            }
            return false;
        }
    }

    @Override
    public boolean isBonemealSuccess(Object thisBlock, Object[] args) {
        return true;
    }

    @Override
    public void performBonemeal(Object thisBlock, Object[] args) {
        Object level = args[0];
        Object randomSource = args[1];
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
            Object headBlockPos = this.getHeadBlockPos(level, blockPos, state);
            if (headBlockPos == null) return;
            Object headBlockState = LevelReaderProxy.INSTANCE.getBlockState(level, headBlockPos);
            Object headBlock = BlockStateProxy.INSTANCE.getBlock(headBlockState);
            if (BonemealableBlockProxy.CLASS.isInstance(headBlock)) {
                BonemealableBlockProxy.INSTANCE.performBonemeal(headBlock, level, randomSource, headBlockPos, headBlockState);
            }
        }
    }

    // 获取头部方块
    private Object getHeadBlockPos(Object level, Object blockPos, Object state) {
        int deep = 1;
        BlockPos currentPos = LocationUtils.fromBlockPos(blockPos);
        for (;;) {
            int nextY = this.direction ? currentPos.y() + deep : currentPos.y() - deep;
            Object nextPos = LocationUtils.toBlockPos(currentPos.x(), nextY, currentPos.z());
            if (LevelHeightAccessorProxy.INSTANCE.isOutsideBuildHeight(level, nextY)) return null;

            Object nextState = BlockGetterProxy.INSTANCE.getBlockState(level, nextPos);
            Optional<ImmutableBlockState> optionalNextBlockState = BlockStateUtils.getOptionalCustomBlockState(nextState);
            Key nextBlockId = optionalNextBlockState.isPresent()
                    ? optionalNextBlockState.get().owner().value().id()
                    : BlockStateUtils.getBlockOwnerIdFromState(nextState);
            Key bodyBlockId = BlockStateUtils.getOptionalCustomBlockState(state).get().owner().value().id();

            // 根茎方块
            if (nextBlockId.equals(bodyBlockId)) {
                deep++;
            }
            // 头方块
            else if (nextBlockId.equals(this.head)) {
                return nextPos;
            }
            // 都不是
            else {
                return null;
            }
        }
    }

    private static class Factory implements BlockBehaviorFactory<VineCropBodyBlockBehavior> {
        private static final String[] BONE_MEAL = new String[] {"bone_meal", "bone-meal"};
        private static final String[] GROW_BLOCKS = new String[] {"grow_blocks", "grow-blocks"};

        @Override
        public VineCropBodyBlockBehavior create(BlockDefinition block, ConfigSection section) {
            boolean boneMealBehavior = false; // false 代表 berries, true 代表 grow
            NumberProvider growBlocks = null;
            ConfigSection boneMealSection = section.getSection(BONE_MEAL);
            if (boneMealSection != null) {
                boneMealBehavior = boneMealSection.getString("behavior", "berries").equalsIgnoreCase("grow");
                if (boneMealBehavior) {
                    growBlocks = boneMealSection.getNumber(GROW_BLOCKS, ConfigConstants.CONSTANT_ONE);
                }
            }

            return new VineCropBodyBlockBehavior(
                    block,
                    BlockBehaviorFactory.getOptionalProperty(block, "berries", Boolean.class),
                    section.getString("direction", "up").equalsIgnoreCase("up"),
                    section.getNonNullIdentifier("head"),
                    section.getInt("delay", 1),
                    boneMealBehavior,
                    growBlocks
            );
        }
    }
}
