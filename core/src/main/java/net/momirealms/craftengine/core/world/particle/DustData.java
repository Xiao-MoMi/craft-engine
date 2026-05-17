package net.momirealms.craftengine.core.world.particle;

import net.momirealms.craftengine.core.util.Color;

public final class DustData implements ParticleData {
    private final Color color;
    private final float size;

    public DustData(Color color, float size) {
        this.color = color;
        this.size = size;
    }

    public Color color() {
        return color;
    }

    public float size() {
        return size;
    }
}
