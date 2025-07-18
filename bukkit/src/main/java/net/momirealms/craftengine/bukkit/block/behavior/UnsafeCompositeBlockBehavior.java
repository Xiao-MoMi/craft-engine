package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.AbstractBlockBehavior;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.item.context.UseOnContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class UnsafeCompositeBlockBehavior extends BukkitBlockBehavior {
    private final AbstractBlockBehavior[] behaviors;

    public UnsafeCompositeBlockBehavior(CustomBlock customBlock, List<AbstractBlockBehavior> behaviors) {
        super(customBlock);
        this.behaviors = behaviors.toArray(new AbstractBlockBehavior[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockBehavior> Optional<T> getAs(Class<T> tClass) {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            if (tClass.isInstance(behavior)) {
                return Optional.of((T) behavior);
            }
        }
        return Optional.empty();
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            InteractionResult result = behavior.useOnBlock(context, state);
            if (result != InteractionResult.PASS && result != InteractionResult.TRY_EMPTY_HAND) {
                return result;
            }
        }
        return super.useOnBlock(context, state);
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            InteractionResult result = behavior.useWithoutItem(context, state);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.useWithoutItem(context, state);
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            state = behavior.updateStateForPlacement(context, state);
            if (state == null) return null;
        }
        return state;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object previous = args[0];
        for (AbstractBlockBehavior behavior : this.behaviors) {
            Object processed = behavior.updateShape(thisBlock, args, superMethod);
            if (processed != previous) {
                return processed;
            }
        }
        return previous;
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.tick(thisBlock, args, superMethod);
        }
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.randomTick(thisBlock, args, superMethod);
        }
    }

    @Override
    public Object rotate(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object previous = args[0];
        for (AbstractBlockBehavior behavior : this.behaviors) {
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
        for (AbstractBlockBehavior behavior : this.behaviors) {
            Object processed = behavior.mirror(thisBlock, args, superMethod);
            if (processed != previous) {
                return processed;
            }
        }
        return previous;
    }

    @Override
    public void performBoneMeal(Object thisBlock, Object[] args) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.performBoneMeal(thisBlock, args);
        }
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.onPlace(thisBlock, args, superMethod);
        }
    }

    @Override
    public void onLand(Object thisBlock, Object[] args) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.onLand(thisBlock, args);
        }
    }

    @Override
    public void onBrokenAfterFall(Object thisBlock, Object[] args) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.onBrokenAfterFall(thisBlock, args);
        }
    }

    @Override
    public void neighborChanged(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.neighborChanged(thisBlock, args, superMethod);
        }
    }

    @Override
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            if (behavior.isValidBoneMealTarget(thisBlock, args)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            if (behavior.isBoneMealSuccess(thisBlock, args)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            if (!behavior.canSurvive(thisBlock, args, superMethod)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            if (!behavior.isPathFindable(thisBlock, args, superMethod)) {
                return false;
            }
        }
        return (boolean) superMethod.call();
    }

    @Override
    public void onExplosionHit(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.onExplosionHit(thisBlock, args, superMethod);
        }
    }

    @Override
    public void setPlacedBy(BlockPlaceContext context, ImmutableBlockState state) {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.setPlacedBy(context, state);
        }
    }

    @Override
    public boolean canBeReplaced(BlockPlaceContext context, ImmutableBlockState state) {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            if (!behavior.canBeReplaced(context, state)) {
                return false;
            }
        }
        return super.canBeReplaced(context, state);
    }

    @Override
    public void entityInside(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.entityInside(thisBlock, args, superMethod);
        }
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.affectNeighborsAfterRemoval(thisBlock, args, superMethod);
        }
    }

    @Override
    public void onRemove(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.onRemove(thisBlock, args, superMethod);
        }
    }

    @Override
    public int getSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            int signal = behavior.getSignal(thisBlock, args, superMethod);
            if (signal != 0) {
                return signal;
            }
        }
        return 0;
    }

    @Override
    public int getDirectSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            int signal = behavior.getDirectSignal(thisBlock, args, superMethod);
            if (signal != 0) {
                return signal;
            }
        }
        return 0;
    }

    @Override
    public boolean isSignalSource(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            if (behavior.isSignalSource(thisBlock, args, superMethod)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object previous = args[0];
        for (AbstractBlockBehavior behavior : this.behaviors) {
            Object processed = behavior.playerWillDestroy(thisBlock, args, superMethod);
            if (processed != previous) {
                return processed;
            }
        }
        return previous;
    }

    @Override
    public void spawnAfterBreak(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        for (AbstractBlockBehavior behavior : this.behaviors) {
            behavior.spawnAfterBreak(thisBlock, args, superMethod);
        }
    }
}
