package net.momirealms.craftengine.core.block.behavior;

public interface BucketPickup {

    /**
     * <code>args[0]</code>: LivingEntity (player) @Nullable<br>
     * <code>args[1]</code>: LevelAccessor (level)<br>
     * <code>args[2]</code>: BlockPos (pos)<br>
     * <code>args[3]</code>: BlockState (state)<br>
     * <p>
     * Returns: ItemStack
     */
    Object pickupBlock(Object thisBlock, Object[] args);

    /**
     * (No arguments)<br>
     * <p>
     * Returns: Optional&lt;SoundEvent&gt;
     */
    Object getPickupSound(Object thisBlock, Object[] args);
}
