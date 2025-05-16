package net.momirealms.craftengine.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.momirealms.craftengine.mod.CraftEnginePlugin;
import net.momirealms.craftengine.mod.util.NoteBlockUtils;
import net.momirealms.craftengine.shared.ObjectHolder;
import net.momirealms.craftengine.shared.block.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class CraftEngineBlock extends Block
        implements BehaviorHolder, ShapeHolder, NoteBlockIndicator, Fallable, BonemealableBlock {
    private static final StoneBlockShape STONE = new StoneBlockShape(Blocks.STONE.defaultBlockState());
    private static final Logger LOGGER = LogManager.getLogger(CraftEngineBlock.class);
    private boolean isNoteBlock;
    public ObjectHolder<BlockBehavior> behaviorHolder;
    public ObjectHolder<BlockShape> shapeHolder;
    public boolean isClientSideNoteBlock;

    public CraftEngineBlock(Properties properties) {
        super(properties);
        this.behaviorHolder = new ObjectHolder<>(EmptyBlockBehavior.INSTANCE);
        this.shapeHolder = new ObjectHolder<>(STONE);
    }

    public void setNoteBlock(boolean noteBlock) {
        this.isNoteBlock = noteBlock;
    }

    @Override
    public ObjectHolder<BlockBehavior> getBehaviorHolder() {
        return this.behaviorHolder;
    }

    @Override
    public ObjectHolder<BlockShape> getShapeHolder() {
        return this.shapeHolder;
    }

    @Override
    public boolean isNoteBlock() {
        return this.isClientSideNoteBlock;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        try {
            return (VoxelShape) this.shapeHolder.value().getShape(this, new Object[]{state, level, pos, context});
        } catch (Exception e) {
            LOGGER.error(e);
            return super.getShape(state, level, pos, context);
        }
    }

    @Override
    protected @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        try {
            return (BlockState) this.behaviorHolder.value().rotate(this, new Object[]{state, rotation}, () -> super.rotate(state, rotation));
        } catch (Exception e) {
            LOGGER.error(e);
            return super.rotate(state, rotation);
        }
    }

    @Override
    protected @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        try {
            return (BlockState) this.behaviorHolder.value().mirror(this, new Object[]{state, mirror}, () -> super.mirror(state, mirror));
        } catch (Exception e) {
            LOGGER.error(e);
            return super.mirror(state, mirror);
        }
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        try {
            this.behaviorHolder.value().tick(this, new Object[]{state, level, pos, random}, () -> {
                 super.tick(state, level, pos, random);
                 return null;
            });
        } catch (Exception e) {
            LOGGER.error(e);
            super.tick(state, level, pos, random);
        }
    }

    @Override
    protected void randomTick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        try {
            behaviorHolder.value().randomTick(this, new Object[]{state, level, pos, random}, () -> {
                super.randomTick(state, level, pos, random);
                return null;
            });
        } catch (Exception e) {
            LOGGER.error(e);
            super.randomTick(state, level, pos, random);
        }
    }

    @Override
    protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
        try {
            behaviorHolder.value().onPlace(this, new Object[]{state, level, pos, oldState, movedByPiston}, () -> {
                super.onPlace(state, level, pos, oldState, movedByPiston);
                return null;
            });
        } catch (Exception e) {
            LOGGER.error(e);
            super.onPlace(state, level, pos, oldState, movedByPiston);
        }
    }

    @Override
    public void onBrokenAfterFall(@NotNull Level level, @NotNull BlockPos pos, @NotNull FallingBlockEntity fallingBlock) {
        try {
            this.behaviorHolder.value().onBrokenAfterFall(this, new Object[]{level, pos, fallingBlock});
        } catch (Exception e) {
            LOGGER.error(e);
            Fallable.super.onBrokenAfterFall(level, pos, fallingBlock);
        }
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        try {
            return this.behaviorHolder.value().canSurvive(this, new Object[]{state, level, pos}, () -> super.canSurvive(state, level, pos));
        } catch (Exception e) {
            LOGGER.error(e);
            return super.canSurvive(state, level, pos);
        }
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state,
                                              @NotNull LevelReader level,
                                              @NotNull ScheduledTickAccess scheduledTickAccess,
                                              @NotNull BlockPos pos,
                                              @NotNull Direction direction,
                                              @NotNull BlockPos neighborPos,
                                              @NotNull BlockState neighborState,
                                              @NotNull RandomSource random) {
        try {
            if (this.isNoteBlock && level instanceof ServerLevel serverLevel) {
                startNoteBlockChain(direction, serverLevel, pos);
            }
            return (BlockState) this.behaviorHolder.value().updateShape(this, new Object[]{state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random}, () -> super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random));
        } catch (Exception e) {
            LOGGER.error(e);
            return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
        }
    }

    private static void startNoteBlockChain(Direction direction, ServerLevel serverLevel, BlockPos blockPos) {
        int id = direction.get3DDataValue();
        // Y axis
        if (id == 0 || id == 1) {
            ServerChunkCache chunkSource = serverLevel.chunkSource;
            chunkSource.blockChanged(blockPos);
            if (id == 1) {
                noteBlockChainUpdate(serverLevel, chunkSource, Direction.DOWN, blockPos, 0);
            } else {
                noteBlockChainUpdate(serverLevel, chunkSource, Direction.UP, blockPos, 0);
            }
        }
    }

    public static void noteBlockChainUpdate(ServerLevel level, ServerChunkCache chunkSource, Direction direction, BlockPos blockPos, int times) {
        if (times >= CraftEnginePlugin.maxChainUpdate()) return;
        BlockPos relativePos = blockPos.relative(direction);
        BlockState state = level.getBlockState(relativePos);
        if (NoteBlockUtils.CLIENT_SIDE_NOTE_BLOCKS.contains(state)) {
            chunkSource.blockChanged(relativePos);
            noteBlockChainUpdate(level, chunkSource, direction, relativePos, times+1);
        }
    }

    @Override
    public boolean isValidBonemealTarget(@NotNull LevelReader levelReader, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        try {
            return this.behaviorHolder.value().isValidBoneMealTarget(this, new Object[]{levelReader, blockPos, blockState});
        } catch (Exception e) {
            LOGGER.error(e);
            return false;
        }
    }

    @Override
    public boolean isBonemealSuccess(@NotNull Level level, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        try {
            return this.behaviorHolder.value().isBoneMealSuccess(this, new Object[]{level, randomSource, blockPos, blockState});
        } catch (Exception e) {
            LOGGER.error(e);
            return false;
        }
    }

    @Override
    public void performBonemeal(@NotNull ServerLevel serverLevel, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        try {
            this.behaviorHolder.value().performBoneMeal(this, new Object[]{serverLevel, randomSource, blockPos, blockState});
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    @Override
    public void onLand(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull BlockState replaceableState, @NotNull FallingBlockEntity fallingBlock) {
        try {
            this.behaviorHolder.value().onLand(this, new Object[]{level, pos, state, replaceableState, fallingBlock});
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }
}
