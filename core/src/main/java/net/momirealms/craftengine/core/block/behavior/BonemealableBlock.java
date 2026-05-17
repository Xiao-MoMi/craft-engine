package net.momirealms.craftengine.core.block.behavior;

public interface BonemealableBlock {

    /**
     * <code>args[0]</code>: LevelReader (level)<br>
     * <code>args[1]</code>: BlockPos (pos)<br>
     * <code>args[2]</code>: BlockState (state)<br>
     * <p>
     * Returns: boolean
     */
    boolean isValidBonemealTarget(Object thisBlock, Object[] args);

    /**
     * <code>args[0]</code>: Level (level)<br>
     * <code>args[1]</code>: RandomSource (random)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: BlockState (state)<br>
     * <p>
     * Returns: boolean
     */
    boolean isBonemealSuccess(Object thisBlock, Object[] args);

    /**
     * <code>args[0]</code>: ServerLevel (level)<br>
     * <code>args[1]</code>: RandomSource (random)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: BlockState (state)<br>
     * <p>
     * Returns: void
     */
    void performBonemeal(Object thisBlock, Object[] args);

    // todo BEHAVIOR
}
