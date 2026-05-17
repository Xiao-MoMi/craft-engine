package net.momirealms.craftengine.core.world.particle;

import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Color;

public final class TrailData implements ParticleData {
    private final NumberProvider targetX;
    private final NumberProvider targetY;
    private final NumberProvider targetZ;
    private final Color color;
    private final NumberProvider duration;

    public TrailData(NumberProvider targetX, NumberProvider targetY, NumberProvider targetZ, Color color, NumberProvider duration) {
        this.color = color;
        this.duration = duration;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public Color color() {
        return color;
    }

    public NumberProvider duration() {
        return duration;
    }

    public NumberProvider targetX() {
        return targetX;
    }

    public NumberProvider targetY() {
        return targetY;
    }

    public NumberProvider targetZ() {
        return targetZ;
    }
}
