package net.momirealms.craftengine.core.block.behavior;

public interface FallableBlock {

    /**
     * <code>args[0]</code>: Level (level)<br>
     * <code>args[1]</code>: BlockPos (pos)<br>
     * <code>args[2]</code>: FallingBlockEntity (fallingBlock)<br>
     * <p>
     * Returns: void
     */
    default void onBrokenAfterFall(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: Level (level)<br>
     * <code>args[1]</code>: BlockPos (pos)<br>
     * <code>args[2]</code>: BlockState (state)<br>
     * <code>args[3]</code>: BlockState (replaceableState)<br>
     * <code>args[4]</code>: FallingBlockEntity (fallingBlock)<br>
     * <p>
     * Returns: void
     */
    default void onLand(Object thisBlock, Object[] args) {
    }

    /**
     * <code>args[0]</code>: Entity (entity)<br>
     * <p>
     * Returns: DamageSource
     */
    Object getFallDamageSource(Object thisBlock, Object[] args);
}
