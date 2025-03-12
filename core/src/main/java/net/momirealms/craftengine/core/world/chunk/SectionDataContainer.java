package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import org.jetbrains.annotations.Nullable;

public class SectionDataContainer {

    private final ImmutableBlockState[] states;

    public SectionDataContainer() {
        this.states = new ImmutableBlockState[4096];
    }

    public SectionDataContainer(ImmutableBlockState[] states) {
        this.states = states;
    }

    public @Nullable ImmutableBlockState get(int index) {
        return states[index];
    }

    public void set(int index, ImmutableBlockState value) {
        states[index] = value;
    }

    public ImmutableBlockState[] states() {
        return states;
    }
}
