package net.momirealms.craftengine.core.block.behavior;

public interface PrioritizedFallOnHandler {

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
    void fallOn(Object thisBlock, Object[] args);

    /**
     * <code>args[0]</code>: BlockGetter (level)<br>
     * <code>args[1]</code>: Entity (entity)<br>
     * <p>
     * Returns: void
     */
    void updateEntityMovementAfterFallOn(Object thisBlock, Object[] args);
}
