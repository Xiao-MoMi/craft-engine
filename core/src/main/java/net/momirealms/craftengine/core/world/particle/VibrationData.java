package net.momirealms.craftengine.core.world.particle;

import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;

public final class VibrationData implements ParticleData {
    private final NumberProvider destinationX;
    private final NumberProvider destinationY;
    private final NumberProvider destinationZ;
    private final NumberProvider arrivalTime;

    public VibrationData(NumberProvider destinationX, NumberProvider destinationY, NumberProvider destinationZ, NumberProvider arrivalTime) {
        this.arrivalTime = arrivalTime;
        this.destinationX = destinationX;
        this.destinationY = destinationY;
        this.destinationZ = destinationZ;
    }

    public NumberProvider arrivalTime() {
        return arrivalTime;
    }

    public NumberProvider destinationX() {
        return destinationX;
    }

    public NumberProvider destinationY() {
        return destinationY;
    }

    public NumberProvider destinationZ() {
        return destinationZ;
    }
}
