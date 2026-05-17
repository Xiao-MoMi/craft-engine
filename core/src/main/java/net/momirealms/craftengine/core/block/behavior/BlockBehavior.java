package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.behavior.BlockItem;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MutableBoolean;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldAccessor;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class BlockBehavior {
    protected final BlockDefinition blockDefinition;

    public BlockBehavior(BlockDefinition blockDefinition) {
        this.blockDefinition = blockDefinition;
    }

    public BlockDefinition block() {
        return this.blockDefinition;
    }

    @SuppressWarnings("unchecked")
    public <T> void let(Class<T> tClass, Consumer<T> consumer) {
        if (tClass.isInstance(this)) {
            consumer.accept((T) this);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T getFirst(Class<T> tClass) {
        if (tClass.isInstance(this)) {
            return (T) this;
        }
        return null;
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Rotation (rotation)<br>
     * <p>
     * Returns: BlockState
     */
    public Object rotate(Object thisBlock, Object[] args) {
        return args[0];
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Mirror (mirror)<br>
     * <p>
     * Returns: BlockState
     */
    public Object mirror(Object thisBlock, Object[] args) {
        return args[0];
    }

    /**
     * --- 1.20.1 - 1.21.1 ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Direction (direction)<br>
     * <code>args[2]</code>: BlockState (neighborState)<br>
     * <code>args[3]</code>: LevelAccessor (world)<br>
     * <code>args[4]</code>: BlockPos (pos)<br>
     * <code>args[5]</code>: BlockPos (neighborPos)<br>
     * <p>
     * --- 1.21.2+ ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: LevelReader (level)<br>
     * <code>args[2]</code>: ScheduledTickAccess (ticks)<br>
     * <code>args[3]</code>: BlockPos (pos)<br>
     * <code>args[4]</code>: Direction (direction)<br>
     * <code>args[5]</code>: BlockPos (neighborPos)<br>
     * <code>args[6]</code>: BlockState (neighborState)<br>
     * <code>args[7]</code>: RandomSource (random)<br>
     * <p>
     * Returns: BlockState
     */
    public Object updateShape(Object thisBlock, Object[] args) {
        return args[0];
    }

    /**
     * --- 1.20.1 - 1.21.1 ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Block (neighborBlock)<br>
     * <code>args[4]</code>: BlockPos (neighborPos)<br>
     * <code>args[5]</code>: boolean (movedByPiston)<br>
     * <p>
     * --- 1.21.2+ ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Block (neighborBlock)<br>
     * <code>args[4]</code>: Orientation (orientation) @Nullable<br>
     * <code>args[5]</code>: boolean (movedByPiston)<br>
     * <p>
     * Returns: void
     */
    public void neighborChanged(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: ServerLevel (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: RandomSource (random)<br>
     * <p>
     * Returns: void
     */
    public void tick(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: ServerLevel (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: RandomSource (random)<br>
     * <p>
     * Returns: void
     */
    public void randomTick(Object thisBlock, Object[] args) {
    }

    /**
     * --- 1.20 - 1.20.4 ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (world)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: BlockState (oldState)<br>
     * <code>args[4]</code>: boolean (notify)<br>
     * <code>args[5]</code>: UseOnContext (context)<br>
     * <p>
     * --- 1.20.5+ ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: BlockState (oldState)<br>
     * <code>args[4]</code>: boolean (movedByPiston)<br>
     * <p>
     * Returns: void
     */
    public void onPlace(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: LevelReader (world)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <p>
     * Returns: boolean
     */
    public boolean canSurvive(Object thisBlock, Object[] args) {
        return true;
    }

    /**
     * --- 1.20 - 1.20.4 ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: BlockGetter (world)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: PathComputationType (type)<br>
     * <p>
     * --- 1.20.5+ ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: PathComputationType (pathComputationType)<br>
     * <p>
     * Returns: boolean
     */
    public abstract boolean isPathFindable(Object thisBlock, Object[] args);

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <p>
     * Returns: boolean
     */
    public boolean hasAnalogOutputSignal(Object thisBlock, Object[] args) {
        return false;
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <p>
     * Returns: int
     */
    public int getAnalogOutputSignal(Object thisBlock, Object[] args) {
        return 0;
    }

    /**
     * --- 1.21+ ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: ServerLevel (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Explosion (explosion)<br>
     * <code>args[4]</code>: BiConsumer&lt;ItemStack, BlockPos&gt; (dropConsumer)<br>
     * <p>
     * Returns: void
     */
    public void preExplosionHit(Object thisBlock, Object[] args) {
    }

    /**
     * --- 1.21+ ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: ServerLevel (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Explosion (explosion)<br>
     * <code>args[4]</code>: BiConsumer&lt;ItemStack, BlockPos&gt; (dropConsumer)<br>
     * <p>
     * Returns: void
     */
    public void postExplosionHit(Object thisBlock, Object[] args) {
    }

    /**
     * --- 1.20 - 1.21.4 ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Entity (entity)<br>
     * <p>
     * --- 1.21.5+ ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Entity (entity)<br>
     * <code>args[4]</code>: InsideBlockEffectApplier (effectApplier)<br>
     * <p>
     * --- 1.21.10+ ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Entity (entity)<br>
     * <code>args[4]</code>: InsideBlockEffectApplier (effectApplier)<br>
     * <code>args[5]</code>: boolean (isPrecise)<br>
     * <p>
     * Returns: void
     */
    public void entityInside(Object thisBlock, Object[] args) {
    }

    /**
     * --- 1.20 - 1.21.4 ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: BlockState (newState)<br>
     * <code>args[4]</code>: boolean (movedByPiston)<br>
     * <p>
     * --- 1.21.5+ ---<br>
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: ServerLevel (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: boolean (movedByPiston)<br>
     * <p>
     * Returns: void
     */
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: BlockGetter (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Direction (direction)<br>
     * <p>
     * Returns: int
     */
    public int getSignal(Object thisBlock, Object[] args) {
        return 0;
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: BlockGetter (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Direction (direction)<br>
     * <p>
     * Returns: int
     */
    public int getDirectSignal(Object thisBlock, Object[] args) {
        return 0;
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <p>
     * Returns: boolean
     */
    public boolean isSignalSource(Object thisBlock, Object[] args) {
        return false;
    }

    /**
     * <code>args[0]</code>: Level (level)<br>
     * <code>args[1]</code>: BlockPos (pos)<br>
     * <code>args[2]</code>: BlockState (state)<br>
     * <code>args[3]</code>: Player (player)<br>
     * <p>
     * Returns: BlockState
     */
    public Object playerWillDestroy(Object thisBlock, Object[] args) {
        return args[2];
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: ServerLevel (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: ItemStack (stack)<br>
     * <code>args[4]</code>: boolean (dropExperience)<br>
     * <p>
     * Returns: void
     */
    public void spawnAfterBreak(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: Level (level)<br>
     * <code>args[1]</code>: BlockPos (pos)<br>
     * <code>args[2]</code>: BlockState (state)<br>
     * <code>args[3]</code>: Entity (entity)<br>
     * <p>
     * Returns: void
     */
    public void stepOn(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: Level (level)<br>
     * <code>args[1]</code>: BlockState (state)<br>
     * <code>args[2]</code>: BlockHitResult (hit)<br>
     * <code>args[3]</code>: Projectile (projectile)<br>
     * <p>
     * Returns: void
     */
    public void onProjectileHit(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: Level / WorldGenLevel (level)<br>
     * <code>args[1]</code>: BlockPos (pos)<br>
     * <code>args[2]</code>: BlockState (state)<br>
     * <code>args[3]</code>: LivingEntity (placer)<br>
     * <code>args[4]</code>: ItemStack (itemStack)<br>
     * <p>
     * Returns: void
     */
    public void placeMultiState(Object thisBlock, Object[] args) {
    }

    /**
     * --- 1.20.1 - 1.21.4 ---<br>
     * <code>args[0]</code>: Level (world)<br>
     * <code>args[1]</code>: BlockState (state)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Entity (entity)<br>
     * <code>args[4]</code>: float (fallDistance)<br>
     * <p>
     * --- 1.21.5+ ---<br>
     * <code>args[0]</code>: Level (level)<br>
     * <code>args[1]</code>: BlockState (state)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Entity (entity)<br>
     * <code>args[4]</code>: double (fallDistance)<br>
     * <p>
     * Returns: void
     */
    public abstract void fallOn(Object thisBlock, Object[] args);

    /**
     * <code>args[0]</code>: BlockGetter (level)<br>
     * <code>args[1]</code>: Entity (entity)<br>
     * <p>
     * Returns: void
     */
    public abstract void updateEntityMovementAfterFallOn(Object thisBlock, Object[] args);

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: int (type)<br>
     * <code>args[4]</code>: int (data)<br>
     * <p>
     * Returns: boolean
     */
    public boolean triggerEvent(Object thisBlock, Object[] args) {
        return false;
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Player (player)<br>
     * <p>
     * Returns: void
     */
    public void attack(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: Level (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: Biome.Precipitation (precipitation)<br>
     * <p>
     * Returns: void
     */
    public void handlePrecipitation(Object thisBlock, Object[] args) {
    }

    public boolean canPlaceMultiState(WorldAccessor accessor, BlockPos pos, ImmutableBlockState state) {
        return true;
    }

    public boolean hasMultiState(ImmutableBlockState baseState) {
        return false;
    }

    public Item itemToPickup(World world, BlockPos pos, ImmutableBlockState state, Player player) {
        return null;
    }

    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        return state;
    }

    public boolean canBeReplaced(BlockPlaceContext context, ImmutableBlockState state) {
        // 如果物品和方块是一家人，那么不应该被替换
        Key clickedBlockId = state.owner().value().id();
        Item item = context.getItem();
        Optional<ItemDefinition> customItem = CraftEngine.instance().itemManager().getItemDefinition(item.id());
        if (customItem.isEmpty()) return state.settings().replaceable();
        ItemDefinition custom = customItem.get();
        MutableBoolean canPlace = new MutableBoolean(true);
        custom.behavior().let(BlockItem.class, b -> {
            Key blockId = b.block();
            if (blockId.equals(clickedBlockId)) {
                canPlace.set(false);
            }
        });
        if (!canPlace.booleanValue()) {
            return false;
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
