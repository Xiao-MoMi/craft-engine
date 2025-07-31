package net.momirealms.craftengine.core.block;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum FluidState {
    EMPTY("empty"),
    WATER("water"),
    FLOWING_WATER("flowing_water"),
    LAVA("lava"),
    FLOWING_LAVA("flowing_lava");

    private final String[] names;

    FluidState(String... names) {
        this.names = names;
    }

    public String[] names() {
        return names;
    }

    private static final Map<String, FluidState> BY_NAME = new HashMap<>();

    static {
        for (FluidState fluidState : FluidState.values()) {
            for (String name : fluidState.names()) {
                BY_NAME.put(name, fluidState);
            }
        }
    }

    public static FluidState byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name)).orElse(EMPTY);
    }
}
