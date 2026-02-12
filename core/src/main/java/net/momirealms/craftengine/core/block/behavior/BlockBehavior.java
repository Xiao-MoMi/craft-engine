package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.BlockBoundItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldAccessor;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.Callable;

public abstract class BlockBehavior {
    protected final CustomBlock customBlock;

    public BlockBehavior(CustomBlock customBlock) {
        this.customBlock = customBlock;
    }

    public CustomBlock block() {
        return this.customBlock;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAs(Class<T> tClass) {
        if (tClass.isInstance(this)) {
            return Optional.of((T) this);
        }
        return Optional.empty();
    }

    @Nullable
    public EntityBlockBehavior getEntityBehavior() {
        if (this instanceof EntityBlockBehavior behavior) {
            return behavior;
        }
        return null;
    }

    // BlockState state, Rotation rotation
    public Object rotate(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    // BlockState state, Mirror mirror
    public Object mirror(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    // 1.20.1-1.21.1 BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos
    // 1.21.2+ BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return args[0];
    }

    // BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston
    public void neighborChanged(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    // BlockState state, ServerLevel level, BlockPos pos, RandomSource random
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    // BlockState state, ServerLevel level, BlockPos pos, RandomSource random
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    // 1.20-1.20.4 BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify, UseOnContext context
    // 1.20.5+ BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston
    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        superMethod.call();
    }

    // BlockState state, LevelReader world, BlockPos pos
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return (boolean) superMethod.call();
    }

    // 1.20-1.20.4 BlockState state, BlockGetter world, BlockPos pos, PathComputationType type
    // 1.20.5+ BlockState state, PathComputationType pathComputationType
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return (boolean) superMethod.call();
    }

    // Level level, BlockPos pos, FallingBlockEntity fallingBlock
    public void onBrokenAfterFall(Object thisBlock, Object[] args) throws Exception {
    }

    // Level level, BlockPos pos, BlockState state, BlockState replaceableState, FallingBlockEntity fallingBlock
    public void onLand(Object thisBlock, Object[] args) throws Exception {
    }

    // LevelReader level, BlockPos pos, BlockState state
    public boolean isValidBoneMealTarget(Object thisBlock, Object[] args) throws Exception {
        return false;
    }

    // BlockState state
    public boolean hasAnalogOutputSignal(Object thisBlock, Object[] args) throws Exception {
        return false;
    }

    // BlockState state, Level level, BlockPos pos
    public int getAnalogOutputSignal(Object thisBlock, Object[] args) throws Exception {
        return 0;
    }

    // BlockState state, LevelAccessor level, BlockPos pos
    public Object getContainer(Object thisBlock, Object[] args) throws Exception {
        return null;
    }

    // Level level, RandomSource random, BlockPos pos, BlockState state
    public boolean isBoneMealSuccess(Object thisBlock, Object[] args) throws Exception {
        return false;
    }

    // ServerLevel level, RandomSource random, BlockPos pos, BlockState state
    public void performBoneMeal(Object thisBlock, Object[] args) throws Exception {
    }

    // 1.21+ BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> dropConsumer
    public void onExplosionHit(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
    }

    // LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState
    public boolean placeLiquid(Object thisObj, Object[] args, Callable<Object> superMethod) {
        return false;
    }

    // 1.20.1 BlockGetter world, BlockPos pos, BlockState state, Fluid fluid
    // 1.20.2+ LivingEntity owner, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid
    public boolean canPlaceLiquid(Object thisObj, Object[] args, Callable<Object> superMethod) {
        return false;
    }

    // 1.20.1 LivingEntity owner, LevelAccessor level, BlockPos pos, BlockState state
    // 1.20.2+ LevelAccessor world, BlockPos pos, BlockState state
    public Object pickupBlock(Object thisObj, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    // 1.20-1.21.4 BlockState state, Level level, BlockPos pos, Entity entity
    // 1.21.5+ BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier
    // 1.21.10+ BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean flag
    public void entityInside(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
    }

    // 1.21.5+ BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
    }

    // 1.20~1.21.4 BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston
    @ApiStatus.Obsolete
    public void onRemove(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
    }

    // BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side
    public int getSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return 0;
    }

    // BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side
    public int getDirectSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return 0;
    }

    // BlockState blockState
    public boolean isSignalSource(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return false;
    }

    // Level level, BlockPos pos, BlockState state, Player player
    public Object playerWillDestroy(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        return superMethod.call();
    }

    // BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean dropExperience
    public void spawnAfterBreak(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
    }

    // Level level, BlockPos pos, BlockState state, Entity entity
    public void stepOn(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
    }

    // Level level, BlockState state, BlockHitResult hit, Projectile projectile
    public void onProjectileHit(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
    }

    // Level/WorldGenLevel level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack
    public void placeMultiState(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
    }

    public boolean canPlaceMultiState(WorldAccessor accessor, BlockPos pos, ImmutableBlockState state) {
        return true;
    }

    public boolean hasMultiState(ImmutableBlockState baseState) {
        return false;
    }

    public Item<?> itemToPickup(World world, BlockPos pos, ImmutableBlockState state, Player player) {
        return null;
    }

    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        return state;
    }

    public boolean canBeReplaced(BlockPlaceContext context, ImmutableBlockState state) {
        Key clickedBlockId = state.owner().value().id();
        Item<?> item = context.getItem();
        Optional<CustomItem<Object>> customItem = CraftEngine.instance().itemManager().getCustomItem(item.id());
        if (customItem.isEmpty()) return state.settings().replaceable();
        CustomItem<Object> custom = customItem.get();
        for (ItemBehavior behavior : custom.behaviors()) {
            if (behavior instanceof BlockBoundItemBehavior blockItemBehavior) {
                Key blockId = blockItemBehavior.block();
                if (blockId.equals(clickedBlockId)) {
                    return false;
                }
            }
        }
        return state.settings().replaceable();
    }

    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        return InteractionResult.TRY_EMPTY_HAND;
    }

    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        return InteractionResult.PASS;
    }
}
