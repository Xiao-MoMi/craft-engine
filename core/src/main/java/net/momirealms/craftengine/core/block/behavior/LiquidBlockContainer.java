package net.momirealms.craftengine.core.block.behavior;

public interface LiquidBlockContainer {

    /**
     * <code>args[0]</code>: LivingEntity (player) @Nullable<br>
     * <code>args[1]</code>: BlockGetter (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: BlockState (state)<br>
     * <code>args[4]</code>: Fluid (fluid)<br>
     * <p>
     * Returns: boolean
     */
    boolean canPlaceLiquid(Object thisBlock, Object[] args);

    /**
     * <code>args[0]</code>: LevelAccessor (level)<br>
     * <code>args[1]</code>: BlockPos (pos)<br>
     * <code>args[2]</code>: BlockState (state)<br>
     * <code>args[3]</code>: FluidState (fluidState)<br>
     * <p>
     * Returns: boolean
     */
    boolean placeLiquid(Object thisBlock, Object[] args);
}
