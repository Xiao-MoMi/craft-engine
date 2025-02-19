package net.momirealms.craftengine.mod;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.momirealms.craftengine.shared.ObjectHolder;
import net.momirealms.craftengine.shared.block.*;
import org.jetbrains.annotations.NotNull;

public class CraftEngineBlock extends Block implements BehaviorHolder, ShapeHolder, NoteBlockIndicator, Fallable, BonemealableBlock {
    private static final MixinStoneBlockShape STONE = new MixinStoneBlockShape(Blocks.STONE.defaultBlockState());
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
        isNoteBlock = noteBlock;
    }

    @Override
    public ObjectHolder<BlockBehavior> getBehaviorHolder() {
        return behaviorHolder;
    }

    @Override
    public ObjectHolder<BlockShape> getShapeHolder() {
        return shapeHolder;
    }

    @Override
    public boolean isNoteBlock() {
        return isClientSideNoteBlock;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        try {
            return (VoxelShape) shapeHolder.value().getShape(this, new Object[]{state, level, pos, context});
        } catch (Exception e) {
            e.printStackTrace();
            return super.getShape(state, level, pos, context);
        }
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        try {
            behaviorHolder.value().tick(this, new Object[]{state, level, pos, random}, () -> {
                 super.tick(state, level, pos, random);
                 return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
            super.onPlace(state, level, pos, oldState, movedByPiston);
        }
    }

    @Override
    public void onBrokenAfterFall(@NotNull Level level, @NotNull BlockPos pos, @NotNull FallingBlockEntity fallingBlock) {
        try {
            behaviorHolder.value().onBrokenAfterFall(this, new Object[]{level, pos, fallingBlock}, () -> {
                Fallable.super.onBrokenAfterFall(level, pos, fallingBlock);
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
            Fallable.super.onBrokenAfterFall(level, pos, fallingBlock);
        }
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        try {
            return behaviorHolder.value().canSurvive(this, new Object[]{state, level, pos}, () -> super.canSurvive(state, level, pos));
        } catch (Exception e) {
            e.printStackTrace();
            return super.canSurvive(state, level, pos);
        }
    }

    @Override
    protected BlockState updateShape(@NotNull BlockState state,
                                     @NotNull LevelReader level,
                                     @NotNull ScheduledTickAccess scheduledTickAccess,
                                     @NotNull BlockPos pos,
                                     @NotNull Direction direction,
                                     @NotNull BlockPos neighborPos,
                                     @NotNull BlockState neighborState,
                                     @NotNull RandomSource random) {
        try {
            if (isNoteBlock && level instanceof ServerLevel serverLevel) {
                startNoteBlockChain(direction, serverLevel, pos);
            }
            return (BlockState) behaviorHolder.value().updateShape(this, new Object[]{state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random}, () -> super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random));
        } catch (Exception e) {
            e.printStackTrace();
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
    protected @NotNull FluidState getFluidState(@NotNull BlockState state) {
        try {
            return (FluidState) behaviorHolder.value().getFluidState(this, new Object[]{state}, () -> super.getFluidState(state));
        } catch (Exception e) {
            e.printStackTrace();
            return super.getFluidState(state);
        }
    }

    @Override
    public boolean isValidBonemealTarget(@NotNull LevelReader levelReader, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        try {
            return behaviorHolder.value().isValidBoneMealTarget(this, new Object[]{levelReader, blockPos, blockState});
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isBonemealSuccess(@NotNull Level level, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        try {
            return behaviorHolder.value().isBoneMealSuccess(this, new Object[]{level, randomSource, blockPos, blockState});
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void performBonemeal(@NotNull ServerLevel serverLevel, @NotNull RandomSource randomSource, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        try {
            behaviorHolder.value().performBoneMeal(this, new Object[]{serverLevel, randomSource, blockPos, blockState});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
