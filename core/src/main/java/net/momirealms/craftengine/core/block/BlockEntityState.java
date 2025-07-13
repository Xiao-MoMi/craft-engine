package net.momirealms.craftengine.core.block;

import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class BlockEntityState {
    private final CompoundTag nbt;

    public BlockEntityState(CompoundTag nbt) {
        this.nbt = nbt.deepClone();
    }

    public CompoundTag nbt() {
        return this.nbt;
    }
}
