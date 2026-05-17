package net.momirealms.craftengine.core.block;

public abstract class AbstractBlockStateWrapper implements BlockStateWrapper {
    protected final Object blockState;
    protected final int registryId;

    protected AbstractBlockStateWrapper(Object blockState, int registryId) {
        this.blockState = blockState;
        this.registryId = registryId;
    }

    @Override
    public Object minecraftState() {
        return this.blockState;
    }

    @Override
    public int registryId() {
        return this.registryId;
    }

    @Override
    public String toString() {
        return this.blockState.toString();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockStateWrapper that)) return false;
        return this.registryId == that.registryId();
    }

    @Override
    public int hashCode() {
        return this.registryId;
    }
}
