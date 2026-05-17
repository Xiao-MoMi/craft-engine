package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.*;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityController;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldAccessor;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class CompositeBlockBehavior extends BukkitBlockBehavior implements CombinedBlockBehavior {
    private final BlockBehavior[] behaviors;

    public CompositeBlockBehavior(BlockDefinition blockDefinition, List<BlockBehavior> behaviors) {
        super(blockDefinition);
        this.behaviors = behaviors.toArray(new BlockBehavior[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @Nullable T getFirst(Class<T> tClass) {
        for (BlockBehavior behavior : this.behaviors) {
            if (tClass.isInstance(behavior)) {
                return (T) behavior;
            }
        }
        return null;
    }

    @Override
    public <T> void let(Class<T> tClass, Consumer<T> consumer) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.let(tClass, consumer);
        }
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        List<BlockEntityController> controllers = new ArrayList<>(this.behaviors.length);
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof EntityBlock entityBlock) {
                controllers.add(entityBlock.createBlockEntityController(blockEntity));
            }
        }
        return BlockEntityController.createFromList(blockEntity, controllers);
    }

    @Override
    public void initControllerId(int id) {
        int index = 0;
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof EntityBlock entityBlock) {
                entityBlock.initControllerId(index++);
            }
        }
    }

    @Override
    public boolean canPlaceLiquid(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof LiquidBlockContainer liquidBlockContainer) {
                return liquidBlockContainer.canPlaceLiquid(thisBlock, args);
            }
        }
        return false;
    }

    @Override
    public boolean placeLiquid(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : behaviors) {
            if (behavior instanceof LiquidBlockContainer liquidBlockContainer) {
                return liquidBlockContainer.placeLiquid(thisBlock, args);
            }
        }
        return false;
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
    public Object updateShape(Object thisBlock, Object[] args) {
        Object previous = args[0];
        for (BlockBehavior behavior : this.behaviors) {
            previous = behavior.updateShape(thisBlock, args);
            args[0] = previous;
        }
        return previous;
    }

    @Override
    public Object getContainer(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof WorldlyContainerHolder worldlyContainerHolder) {
                Object container = worldlyContainerHolder.getContainer(thisBlock, args);
                if (container != null) return container;
            }
        }
        return null;
    }

    @Override
    public void tick(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.tick(thisBlock, args);
        }
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.randomTick(thisBlock, args);
        }
    }

    @Override
    public Object rotate(Object thisBlock, Object[] args) {
        Object previous = args[0];
        for (BlockBehavior behavior : this.behaviors) {
            Object rotated = behavior.rotate(thisBlock, args);
            if (rotated != previous) {
                return rotated;
            }
        }
        return super.rotate(thisBlock, args);
    }

    @Override
    public Object mirror(Object thisBlock, Object[] args) {
        Object previous = args[0];
        for (BlockBehavior behavior : this.behaviors) {
            Object mirrored = behavior.mirror(thisBlock, args);
            if (mirrored != previous) {
                return mirrored;
            }
        }
        return super.mirror(thisBlock, args);
    }

    @Override
    public void performBonemeal(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof BonemealableBlock bonemealableBlock) {
                // 额外检测，避免逻辑错误
                if (isBonemealSuccess(behavior, args)) {
                    bonemealableBlock.performBonemeal(thisBlock, args);
                }
            }
        }
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.onPlace(thisBlock, args);
        }
    }

    @Override
    public void onLand(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof FallableBlock fallable) {
                fallable.onLand(thisBlock, args);
            }
        }
    }

    @Override
    public void onBrokenAfterFall(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof FallableBlock fallable) {
                fallable.onBrokenAfterFall(thisBlock, args);
            }
        }
    }

    @Override
    public void neighborChanged(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.neighborChanged(thisBlock, args);
        }
    }

    @Override
    public boolean isValidBonemealTarget(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof BonemealableBlock bonemealableBlock) {
                if (bonemealableBlock.isValidBonemealTarget(thisBlock, args)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof BonemealableBlock bonemealableBlock) {
                if (bonemealableBlock.isBonemealSuccess(thisBlock, args)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (!behavior.canSurvive(thisBlock, args)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        boolean processed = false;
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof PathFindingBlock pathFindableBlockBehavior) {
                if (!pathFindableBlockBehavior.isPathFindable(thisBlock, args)) {
                    return false;
                } else {
                    processed = true;
                }
            }
        }
        if (!processed) {
            return super.isPathFindable(thisBlock, args);
        }
        return true;
    }

    @Override
    public void preExplosionHit(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.preExplosionHit(thisBlock, args);
        }
    }

    @Override
    public void postExplosionHit(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.postExplosionHit(thisBlock, args);
        }
    }

    @Override
    public Item itemToPickup(World world, BlockPos pos, ImmutableBlockState state, Player player) {
        for (BlockBehavior behavior : this.behaviors) {
            Item item = behavior.itemToPickup(world, pos, state, player);
            if (item != null && !item.isEmpty()) return item;
        }
        return super.itemToPickup(world, pos, state, player);
    }

    @Override
    public boolean canBeReplaced(BlockPlaceContext context, ImmutableBlockState state) {
        // 有一个不能被替换，那就都不能被替换
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior.canBeReplaced(context, state)) return true;
        }
        return false;
    }

    @Override
    public void entityInside(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.entityInside(thisBlock, args);
        }
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.affectNeighborsAfterRemoval(thisBlock, args);
        }
    }

    @Override
    public int getSignal(Object thisBlock, Object[] args) {
        int max = 0;
        for (BlockBehavior behavior : this.behaviors) {
            int signal = behavior.getSignal(thisBlock, args);
            max = Math.max(signal, max);
        }
        return max;
    }

    @Override
    public int getDirectSignal(Object thisBlock, Object[] args) {
        int max = 0;
        for (BlockBehavior behavior : this.behaviors) {
            int signal = behavior.getDirectSignal(thisBlock, args);
            max = Math.max(signal, max);
        }
        return max;
    }

    @Override
    public int getAnalogOutputSignal(Object thisBlock, Object[] args) {
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
    public boolean isSignalSource(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior.isSignalSource(thisBlock, args)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnalogOutputSignal(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior.hasAnalogOutputSignal(thisBlock, args)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args) {
        Object previous = args[2];
        Object differentState = previous;
        for (BlockBehavior behavior : this.behaviors) {
            Object different = behavior.playerWillDestroy(thisBlock, args);
            if (different != previous) {
                differentState = different;
            }
        }
        return differentState;
    }

    @Override
    public void spawnAfterBreak(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.spawnAfterBreak(thisBlock, args);
        }
    }

    @Override
    public void fallOn(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof PrioritizedFallOnHandler f) {
                f.fallOn(thisBlock, args);
                return;
            }
        }
        super.fallOn(thisBlock, args);
    }

    @Override
    public void updateEntityMovementAfterFallOn(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof PrioritizedFallOnHandler f) {
                f.updateEntityMovementAfterFallOn(thisBlock, args);
                return;
            }
        }
        super.updateEntityMovementAfterFallOn(thisBlock, args);
    }

    @Override
    public void stepOn(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.stepOn(thisBlock, args);
        }
    }

    @Override
    public void onProjectileHit(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.onProjectileHit(thisBlock, args);
        }
    }

    @Override
    public void placeMultiState(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.placeMultiState(thisBlock, args);
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

    @Override
    public Object pickupBlock(Object thisObj, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof BucketPickup bucketPickup) {
                Object o = bucketPickup.pickupBlock(thisObj, args);
                if (o != null && !ItemStackProxy.INSTANCE.isEmpty(o)) {
                    return o;
                }
            }
        }
        return ItemStackProxy.EMPTY;
    }

    @Override
    public Object getFallDamageSource(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof FallableBlock fallable) {
                return fallable.getFallDamageSource(thisBlock, args);
            }
        }
        return CombinedBlockBehavior.super.getFallDamageSource(thisBlock, args);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getPickupSound(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof BucketPickup bucketPickup) {
                Optional<Object> pickupSound = (Optional<Object>) bucketPickup.getPickupSound(thisBlock, args);
                if (pickupSound.isPresent()) {
                    return pickupSound;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void attack(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.attack(thisBlock, args);
        }
    }

    @Override
    public void handlePrecipitation(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            behavior.handlePrecipitation(thisBlock, args);
        }
    }

    @Override
    public boolean triggerEvent(Object thisBlock, Object[] args) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior.triggerEvent(thisBlock, args)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        for (BlockBehavior behavior : this.behaviors) {
            if (behavior instanceof RandomTickBlock randomTickBlock && randomTickBlock.canRandomlyTick(state)) {
                return true;
            }
        }
        return false;
    }
}
