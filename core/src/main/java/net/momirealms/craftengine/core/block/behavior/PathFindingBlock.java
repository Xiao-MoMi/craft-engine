package net.momirealms.craftengine.core.block.behavior;

public interface PathFindingBlock {

    /**
     * <code>args[0]</code>: BlockState (state)<br>
     * <code>args[1]</code>: PathComputationType (type)<br>
     * <p>
     * Returns: boolean
     */
    boolean isPathFindable(Object thisBlock, Object[] args);
}
