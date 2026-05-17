package net.momirealms.craftengine.core.block.behavior;

public interface WorldlyContainerHolder {

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: LevelAccessor (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <p>
     * Returns: WorldlyContainer
     */
    Object getContainer(Object thisBlock, Object[] args);
}
