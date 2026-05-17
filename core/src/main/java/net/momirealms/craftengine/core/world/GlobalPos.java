package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.util.Key;

import java.util.Objects;

public final class GlobalPos {
    public final Key dimension;
    public final BlockPos pos;

    private GlobalPos(Key dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos;
    }

    public static GlobalPos of(Key dimension, BlockPos pos) {
        return new GlobalPos(dimension, pos);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        GlobalPos globalPos = (GlobalPos) object;
        return Objects.equals(dimension, globalPos.dimension) && Objects.equals(pos, globalPos.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.dimension, this.pos);
    }

    @Override
    public String toString() {
        return this.dimension + " " + this.pos;
    }
}
