package net.momirealms.craftengine.core.world.particle;

import net.momirealms.craftengine.core.util.Color;

public final class ColorData implements ParticleData {
    private final Color color;

    public ColorData(Color color) {
        this.color = color;
    }

    public Color color() {
        return color;
    }
}
