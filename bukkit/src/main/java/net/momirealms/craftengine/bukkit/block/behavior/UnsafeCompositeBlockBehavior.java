package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.*;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.WorldAccessor;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class UnsafeCompositeBlockBehavior extends BukkitBlockBehavior
        implements FallOnBlockBehavior, PlaceLiquidBlockBehavior, IsPathFindableBlockBehavior, CanBeReplacedBlockBehavior {
    private final BlockBehavior[] behaviors;

    public UnsafeCompositeBlockBehavior(CustomBlock customBlock, List<BlockBehavior> behaviors) {
        super(customBlock);
        this.behaviors = behaviors.toArray(new BlockBehavior[0]);
    }

    @Override
    public boolean canPlaceLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        for (BlockBehavior behavior : behaviors) {
            if (behavior instanceof PlaceLiquidBlockBehavior) {
                return behavior.canPlaceLiquid(thisBlock, args, superMethod);
            }
        }
        return super.canPlaceLiquid(thisBlock, args, superMethod);
    }

    @Override
    public boolean placeLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        for (BlockBehavior behavior : behaviors) {
            if (behavior instanceof PlaceLiquidBlockBehavior) {
                return behavior.placeLiquid(thisBlock, args, superMethod);
            }
        }
        return super.placeLiquid(thisBlock, args, superMethod);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getAs(Class<T> tClass) {
        for (BlockBehavior behavior : this.behaviors) {
            if (tClass.isInstance(behavior)) {
                return Optional.of((T) behavior);
            }
        }
        return Optional.empty();
    }

    @Nullable
    @Override
    public EntityBlockBehavior getEntityBehavior() {
        EntityBlockBehavior target = null;
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof EntityBlockBehavior entityBehavior) {
                if (target == null) {
                    target = entityBehavior;
                } else {
                    throw new IllegalArgumentException("Multiple entity block behaviors are not allowed");
                }
            }
        }
        return target;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        boolean hasPass = false;
        for (BlockBehavior behavior : this.behaviors) {
            InteractionResult result = behavior.useOnBlock(context, state);
            if (result == InteractionResult.PASS) {
                hasPass = true;
                continue;
            }
            if (result != InteractionResult.TRY_EMPTY_HAND) {
                return result;
            }
        }
        return hasPass ? InteractionResult.PASS : super.useOnBlock(context, state);
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        for (BlockBehavior behavior : this.behaviors) {
            InteractionResult result = behavior.useWithoutItem(context, state);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.useWithoutItem(context, state);
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        for (BlockBehavior behavior : this.behaviors) {
            state = behavior.updateStateForPlacement(context, state);
            if (state == null) return null;
        }
        return state;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object previous = args[0];
        for (BlockBehavior behavior : this.behaviors) {
            Object processed = behavior.updateShape(thisBlock, args, superMethod);
            if (processed != previous) {
                return processed;
            }
        }
        return previous;
    }

    @Override
    public Object getContainer(Object thisBlock, Object[] args) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            Object container = behavior.getContainer(thisBlock, args);
            if (container != null) {
                return container;
            }
        }
        return null;
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.tick(thisBlock, args, superMethod);
        }
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.randomTick(thisBlock, args, superMethod);
        }
    }

    @Override
    public Object rotate(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object previous = args[0];
        for (BlockBehavior behavior : this.behaviors) {
            Object processed = behavior.rotate(thisBlock, args, superMethod);
            if (processed != previous) {
                return processed;
            }
        }
        return previous;
    }

    @Override
    public Object mirror(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object previous = args[0];
        for (BlockBehavior behavior : this.behaviors) {
            Object processed = behavior.mirror(thisBlock, args, superMethod);
            if (processed != previous) {
                return processed;
            }
        }
        return previous;
    }

    @Override
    public void performBoneMeal(Object thisBlock, Object[] args) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.performBoneMeal(thisBlock, args);
        }
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.onPlace(thisBlock, args, superMethod);
        }
    }

    @Override
    public void onLand(Object thisBlock, Object[] args) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.onLand(thisBlock, args);
        }
    }

    @Override
    public void onBrokenAfterFall(Object thisBlock, Object[] args) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.onBrokenAfterFall(thisBlock, args);
        }
    }

    @Override
    public void neighborChanged(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.neighborChanged(thisBlock, args, superMethod);
        }
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior.isValidBoneMealTarget(thisBlock, args)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior.isBoneMealSuccess(thisBlock, args)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            if (!behavior.canSurvive(thisBlock, args, superMethod)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        boolean processed = false;
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof IsPathFindableBlockBehavior pathFindableBlockBehavior) {
                if (!pathFindableBlockBehavior.isPathFindable(thisBlock, args, superMethod)) {
                    return false;
                } else {
                    processed = true;
                }
            }
        }
        if (!processed) return (boolean) superMethod.call();
        return true;
    }

    @Override
    public void onExplosionHit(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.onExplosionHit(thisBlock, args, superMethod);
        }
    }

    @Override
    public boolean canBeReplaced(BlockPlaceContext context, ImmutableBlockState state) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof CanBeReplacedBlockBehavior canBeReplacedBlockBehavior) {
                return canBeReplacedBlockBehavior.canBeReplaced(context, state);
            }
        }
        return super.canBeReplaced(context, state);
    }

    @Override
    public void entityInside(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.entityInside(thisBlock, args, superMethod);
        }
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.affectNeighborsAfterRemoval(thisBlock, args, superMethod);
        }
    }

    @Override
    public void onRemove(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.onRemove(thisBlock, args, superMethod);
        }
    }

    @Override
    public int getSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        for (BlockBehavior behavior : this.behaviors) {
            int signal = behavior.getSignal(thisBlock, args, superMethod);
            if (signal != 0) {
                return signal;
            }
        }
        return 0;
    }

    @Override
    public int getDirectSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        for (BlockBehavior behavior : this.behaviors) {
            int signal = behavior.getDirectSignal(thisBlock, args, superMethod);
            if (signal != 0) {
                return signal;
            }
        }
        return 0;
    }

    @Override
    public boolean isSignalSource(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior.isSignalSource(thisBlock, args, superMethod)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnalogOutputSignal(Object thisBlock, Object[] args) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior.hasAnalogOutputSignal(thisBlock, args)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getAnalogOutputSignal(Object thisBlock, Object[] args) throws Exception {
        int signal = 0;
        int count = 0;
        for (BlockBehavior behavior : this.behaviors) {
            int s = behavior.getAnalogOutputSignal(thisBlock, args);
            if (s != 0) {
                signal += s;
                count++;
            }
        }
        return count == 0 ? 0 : signal / count;
    }

    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object previous = args[0];
        for (BlockBehavior behavior : this.behaviors) {
            Object processed = behavior.playerWillDestroy(thisBlock, args, superMethod);
            if (processed != previous) {
                return processed;
            }
        }
        return previous;
    }

    @Override
    public void spawnAfterBreak(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.spawnAfterBreak(thisBlock, args, superMethod);
        }
    }

    @Override
    public void fallOn(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof FallOnBlockBehavior f) {
                f.fallOn(thisBlock, args, superMethod);
                return;
            }
        }
        FallOnBlockBehavior.super.fallOn(thisBlock, args, superMethod);
    }

    @Override
    public void updateEntityMovementAfterFallOn(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof FallOnBlockBehavior f) {
                f.updateEntityMovementAfterFallOn(thisBlock, args, superMethod);
                return;
            }
        }
        FallOnBlockBehavior.super.updateEntityMovementAfterFallOn(thisBlock, args, superMethod);
    }

    @Override
    public void stepOn(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.stepOn(thisBlock, args, superMethod);
        }
    }

    @Override
    public void onProjectileHit(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.onProjectileHit(thisBlock, args, superMethod);
        }
    }

    @Override
    public void placeMultiState(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.placeMultiState(thisBlock, args, superMethod);
        }
    }

    @Override
    public boolean canPlaceMultiState(WorldAccessor accessor, BlockPos pos, ImmutableBlockState state) {
        for (BlockBehavior behavior : this.behaviors) {
            if (!behavior.canPlaceMultiState(accessor, pos, state)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasMultiState(ImmutableBlockState baseState) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior.hasMultiState(baseState)) {
                return true;
            }
        }
        return false;
    }
}
