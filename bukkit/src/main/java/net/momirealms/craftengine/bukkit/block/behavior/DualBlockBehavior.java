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

public final class DualBlockBehavior extends BukkitBlockBehavior implements CombinedBlockBehavior {
    private final BlockBehavior first;
    private final BlockBehavior second;

    public DualBlockBehavior(BlockDefinition blockDefinition, BlockBehavior first, BlockBehavior second) {
        super(blockDefinition);
        this.first = first;
        this.second = second;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @Nullable T getFirst(Class<T> tClass) {
        if (tClass.isInstance(this.first)) return (T) this.first;
        if (tClass.isInstance(this.second)) return (T) this.second;
        return null;
    }

    @Override
    public <T> void let(Class<T> tClass, Consumer<T> consumer) {
        this.first.let(tClass, consumer);
        this.second.let(tClass, consumer);
    }

    @Override
    public BlockEntityController createBlockEntityController(BlockEntity blockEntity) {
        List<BlockEntityController> controllers = new ArrayList<>(2);
        if (this.first instanceof EntityBlock eb1) {
            controllers.add(eb1.createBlockEntityController(blockEntity));
        }
        if (this.second instanceof EntityBlock eb2) {
            controllers.add(eb2.createBlockEntityController(blockEntity));
        }
        return BlockEntityController.createFromList(blockEntity, controllers);
    }

    @Override
    public void initControllerId(int id) {
        int index = 0;
        if (this.first instanceof EntityBlock eb1) {
            eb1.initControllerId(index++);
        }
        if (this.second instanceof EntityBlock eb2) {
            eb2.initControllerId(index);
        }
    }

    @Override
    public boolean canPlaceLiquid(Object thisBlock, Object[] args) {
        if (this.first instanceof LiquidBlockContainer lbc1) {
            return lbc1.canPlaceLiquid(thisBlock, args);
        }
        if (this.second instanceof LiquidBlockContainer lbc2) {
            return lbc2.canPlaceLiquid(thisBlock, args);
        }
        return false;
    }

    @Override
    public boolean placeLiquid(Object thisBlock, Object[] args) {
        if (this.first instanceof LiquidBlockContainer lbc1) {
            return lbc1.placeLiquid(thisBlock, args);
        }
        if (this.second instanceof LiquidBlockContainer lbc2) {
            return lbc2.placeLiquid(thisBlock, args);
        }
        return false;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        InteractionResult r1 = this.first.useOnBlock(context, state);
        boolean hasPass = (r1 == InteractionResult.PASS);
        if (!hasPass && r1 != InteractionResult.TRY_EMPTY_HAND) {
            return r1;
        }

        InteractionResult r2 = this.second.useOnBlock(context, state);
        if (r2 == InteractionResult.PASS) {
            hasPass = true;
        } else if (r2 != InteractionResult.TRY_EMPTY_HAND) {
            return r2;
        }

        return hasPass ? InteractionResult.PASS : super.useOnBlock(context, state);
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        InteractionResult r1 = this.first.useWithoutItem(context, state);
        if (r1 != InteractionResult.PASS) return r1;
        InteractionResult r2 = this.second.useWithoutItem(context, state);
        if (r2 != InteractionResult.PASS) return r2;
        return super.useWithoutItem(context, state);
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        state = this.first.updateStateForPlacement(context, state);
        if (state == null) return null;
        return this.second.updateStateForPlacement(context, state);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        args[0] = this.first.updateShape(thisBlock, args);
        return this.second.updateShape(thisBlock, args);
    }

    @Override
    public Object getContainer(Object thisBlock, Object[] args) {
        if (this.first instanceof WorldlyContainerHolder wch1) {
            Object c1 = wch1.getContainer(thisBlock, args);
            if (c1 != null) return c1;
        }
        if (this.second instanceof WorldlyContainerHolder wch2) {
            Object c2 = wch2.getContainer(thisBlock, args);
            if (c2 != null) return c2;
        }
        return null;
    }

    @Override
    public void tick(Object thisBlock, Object[] args) {
        this.first.tick(thisBlock, args);
        this.second.tick(thisBlock, args);
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        this.first.randomTick(thisBlock, args);
        this.second.randomTick(thisBlock, args);
    }

    @Override
    public Object rotate(Object thisBlock, Object[] args) {
        Object previous = args[0];
        Object rotated1 = this.first.rotate(thisBlock, args);
        if (rotated1 != previous) return rotated1;
        Object rotated2 = this.second.rotate(thisBlock, args);
        if (rotated2 != previous) return rotated2;
        return super.rotate(thisBlock, args);
    }

    @Override
    public Object mirror(Object thisBlock, Object[] args) {
        Object previous = args[0];
        Object mirrored1 = this.first.mirror(thisBlock, args);
        if (mirrored1 != previous) return mirrored1;
        Object mirrored2 = this.second.mirror(thisBlock, args);
        if (mirrored2 != previous) return mirrored2;
        return super.mirror(thisBlock, args);
    }

    @Override
    public void performBonemeal(Object thisBlock, Object[] args) {
        if (this.first instanceof BonemealableBlock b1) {
            if (b1.isBonemealSuccess(thisBlock, args)) {
                b1.performBonemeal(thisBlock, args);
            }
        }
        if (this.second instanceof BonemealableBlock b2) {
            if (b2.isBonemealSuccess(thisBlock, args)) {
                b2.performBonemeal(thisBlock, args);
            }
        }
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args) {
        this.first.onPlace(thisBlock, args);
        this. second.onPlace(thisBlock, args);
    }

    @Override
    public void onLand(Object thisBlock, Object[] args) {
        if (this.first instanceof FallableBlock f1) {
            f1.onLand(thisBlock, args);
        }
        if (this.second instanceof FallableBlock f2) {
            f2.onLand(thisBlock, args);
        }
    }

    @Override
    public void onBrokenAfterFall(Object thisBlock, Object[] args) {
        if (this.first instanceof FallableBlock f1) {
            f1.onBrokenAfterFall(thisBlock, args);
        }
        if (this.second instanceof FallableBlock f2) {
            f2.onBrokenAfterFall(thisBlock, args);
        }
    }

    @Override
    public void neighborChanged(Object thisBlock, Object[] args) {
        this.first.neighborChanged(thisBlock, args);
        this.second.neighborChanged(thisBlock, args);
    }

    @Override
    public boolean isValidBonemealTarget(Object thisBlock, Object[] args) {
        if (this.first instanceof BonemealableBlock b1 && b1.isValidBonemealTarget(thisBlock, args)) return true;
        if (this.second instanceof BonemealableBlock b2 && b2.isValidBonemealTarget(thisBlock, args)) return true;
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Object thisBlock, Object[] args) {
        if (this.first instanceof BonemealableBlock b1 && b1.isBonemealSuccess(thisBlock, args)) return true;
        if (this.second instanceof BonemealableBlock b2 && b2.isBonemealSuccess(thisBlock, args)) return true;
        return false;
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args) {
        return this.first.canSurvive(thisBlock, args) && this.second.canSurvive(thisBlock, args);
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        boolean processed = false;
        if (this.first instanceof PathFindingBlock p1) {
            if (!p1.isPathFindable(thisBlock, args)) return false;
            processed = true;
        }
        if (this.second instanceof PathFindingBlock p2) {
            if (!p2.isPathFindable(thisBlock, args)) return false;
            processed = true;
        }
        return processed || super.isPathFindable(thisBlock, args);
    }

    @Override
    public void preExplosionHit(Object thisBlock, Object[] args) {
        this.first.preExplosionHit(thisBlock, args);
        this.second.preExplosionHit(thisBlock, args);
    }

    @Override
    public void postExplosionHit(Object thisBlock, Object[] args) {
        this.first.postExplosionHit(thisBlock, args);
        this.second.postExplosionHit(thisBlock, args);
    }

    @Override
    public Item itemToPickup(World world, BlockPos pos, ImmutableBlockState state, Player player) {
        Item i1 = this.first.itemToPickup(world, pos, state, player);
        if (i1 != null && !i1.isEmpty()) return i1;
        Item i2 = this.second.itemToPickup(world, pos, state, player);
        if (i2 != null && !i2.isEmpty()) return i2;
        return super.itemToPickup(world, pos, state, player);
    }

    @Override
    public boolean canBeReplaced(BlockPlaceContext context, ImmutableBlockState state) {
        return this.first.canBeReplaced(context, state) || this.second.canBeReplaced(context, state);
    }

    @Override
    public void entityInside(Object thisBlock, Object[] args) {
        this.first.entityInside(thisBlock, args);
        this.second.entityInside(thisBlock, args);
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args) {
        this.first.affectNeighborsAfterRemoval(thisBlock, args);
        this.second.affectNeighborsAfterRemoval(thisBlock, args);
    }

    @Override
    public int getSignal(Object thisBlock, Object[] args) {
        return Math.max(this.first.getSignal(thisBlock, args), this.second.getSignal(thisBlock, args));
    }

    @Override
    public int getDirectSignal(Object thisBlock, Object[] args) {
        return Math.max(this.first.getDirectSignal(thisBlock, args), this.second.getDirectSignal(thisBlock, args));
    }

    @Override
    public int getAnalogOutputSignal(Object thisBlock, Object[] args) {
        int s1 = this.first.getAnalogOutputSignal(thisBlock, args);
        int s2 = this.second.getAnalogOutputSignal(thisBlock, args);
        if (s1 != 0 && s2 != 0) return (s1 + s2) / 2;
        if (s1 != 0) return s1;
        if (s2 != 0) return s2;
        return 0;
    }

    @Override
    public boolean isSignalSource(Object thisBlock, Object[] args) {
        return this.first.isSignalSource(thisBlock, args) || this.second.isSignalSource(thisBlock, args);
    }

    @Override
    public boolean hasAnalogOutputSignal(Object thisBlock, Object[] args) {
        return this.first.hasAnalogOutputSignal(thisBlock, args) || this.second.hasAnalogOutputSignal(thisBlock, args);
    }

    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args) {
        Object previous = args[2];
        Object res1 = this.first.playerWillDestroy(thisBlock, args);
        Object res2 = this.second.playerWillDestroy(thisBlock, args);
        if (res2 != previous) return res2;
        if (res1 != previous) return res1;
        return previous;
    }

    @Override
    public void spawnAfterBreak(Object thisBlock, Object[] args) {
        this.first.spawnAfterBreak(thisBlock, args);
        this.second.spawnAfterBreak(thisBlock, args);
    }

    @Override
    public void fallOn(Object thisBlock, Object[] args) {
        if (this.first instanceof PrioritizedFallOnHandler f1) {
            f1.fallOn(thisBlock, args);
            return;
        }
        if (this.second instanceof PrioritizedFallOnHandler f2) {
            f2.fallOn(thisBlock, args);
            return;
        }
        super.fallOn(thisBlock, args);
    }

    @Override
    public void updateEntityMovementAfterFallOn(Object thisBlock, Object[] args) {
        if (this.first instanceof PrioritizedFallOnHandler f1) {
            f1.updateEntityMovementAfterFallOn(thisBlock, args);
            return;
        }
        if (this.second instanceof PrioritizedFallOnHandler f2) {
            f2.updateEntityMovementAfterFallOn(thisBlock, args);
            return;
        }
        super.updateEntityMovementAfterFallOn(thisBlock, args);
    }

    @Override
    public void stepOn(Object thisBlock, Object[] args) {
        this.first.stepOn(thisBlock, args);
        this.second.stepOn(thisBlock, args);
    }

    @Override
    public void onProjectileHit(Object thisBlock, Object[] args) {
        this.first.onProjectileHit(thisBlock, args);
        this.second.onProjectileHit(thisBlock, args);
    }

    @Override
    public void placeMultiState(Object thisBlock, Object[] args) {
        this.first.placeMultiState(thisBlock, args);
        this.second.placeMultiState(thisBlock, args);
    }

    @Override
    public boolean canPlaceMultiState(WorldAccessor accessor, BlockPos pos, ImmutableBlockState state) {
        return this.first.canPlaceMultiState(accessor, pos, state) && this.second.canPlaceMultiState(accessor, pos, state);
    }

    @Override
    public boolean hasMultiState(ImmutableBlockState baseState) {
        return this.first.hasMultiState(baseState) || this.second.hasMultiState(baseState);
    }

    @Override
    public Object pickupBlock(Object thisObj, Object[] args) {
        if (this.first instanceof BucketPickup bp1) {
            Object o1 = bp1.pickupBlock(thisObj, args);
            if (o1 != null && !ItemStackProxy.INSTANCE.isEmpty(o1)) return o1;
        }
        if (this.second instanceof BucketPickup bp2) {
            Object o2 = bp2.pickupBlock(thisObj, args);
            if (o2 != null && !ItemStackProxy.INSTANCE.isEmpty(o2)) return o2;
        }
        return ItemStackProxy.EMPTY;
    }

    @Override
    public Object getFallDamageSource(Object thisBlock, Object[] args) {
        if (this.first instanceof FallableBlock f1) {
            return f1.getFallDamageSource(thisBlock, args);
        }
        if (this.second instanceof FallableBlock f2) {
            return f2.getFallDamageSource(thisBlock, args);
        }
        return CombinedBlockBehavior.super.getFallDamageSource(thisBlock, args);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getPickupSound(Object thisBlock, Object[] args) {
        if (this.first instanceof BucketPickup bp1) {
            Optional<Object> s1 = (Optional<Object>) bp1.getPickupSound(thisBlock, args);
            if (s1.isPresent()) return s1;
        }
        if (this.second instanceof BucketPickup bp2) {
            Optional<Object> s2 = (Optional<Object>) bp2.getPickupSound(thisBlock, args);
            if (s2.isPresent()) return s2;
        }
        return Optional.empty();
    }

    @Override
    public void attack(Object thisBlock, Object[] args) {
        this.first.attack(thisBlock, args);
        this.second.attack(thisBlock, args);
    }

    @Override
    public void handlePrecipitation(Object thisBlock, Object[] args) {
        this.first.handlePrecipitation(thisBlock, args);
        this.second.handlePrecipitation(thisBlock, args);
    }

    @Override
    public boolean triggerEvent(Object thisBlock, Object[] args) {
        return this.first.triggerEvent(thisBlock, args) || this.second.triggerEvent(thisBlock, args);
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        if (this.first instanceof RandomTickBlock randomTickBlock && randomTickBlock.canRandomlyTick(state)) return true;
        if (this.second instanceof RandomTickBlock randomTickBlock && randomTickBlock.canRandomlyTick(state)) return true;
        return false;
    }
}