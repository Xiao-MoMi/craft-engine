package net.momirealms.craftengine.core.world.particle;

import net.momirealms.craftengine.core.util.Color;

public final class DustTransitionData implements ParticleData {
    private final Color from;
    private final Color to;
    private final float size;

    public DustTransitionData(Color from, Color to, float size) {
        this.from = from;
        this.to = to;
        this.size = size;
    }

    public Color from() {
        return from;
    }

    public Color to() {
        return to;
    }

    public float size() {
        return size;
    }
}
