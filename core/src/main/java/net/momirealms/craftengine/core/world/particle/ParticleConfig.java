package net.momirealms.craftengine.core.world.particle;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public final class ParticleConfig {
    public final ParticleType particleType;
    public final NumberProvider x;
    public final NumberProvider y;
    public final NumberProvider z;
    public final NumberProvider count;
    public final NumberProvider xOffset;
    public final NumberProvider yOffset;
    public final NumberProvider zOffset;
    public final NumberProvider speed;
    public final ParticleData particleData;

    public ParticleConfig(ParticleType particleType,
                          NumberProvider x,
                          NumberProvider y,
                          NumberProvider z,
                          NumberProvider count,
                          NumberProvider xOffset,
                          NumberProvider yOffset,
                          NumberProvider zOffset,
                          NumberProvider speed,
                          ParticleData particleData) {
        this.particleType = particleType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.count = count;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.speed = speed;
        this.particleData = particleData;
    }

    private static final String[] OFFSET_X = new String[] {"offset_x", "offset-x"};
    private static final String[] OFFSET_Y = new String[] {"offset_y", "offset-y"};
    private static final String[] OFFSET_Z = new String[] {"offset_z", "offset-z"};

    public static ParticleConfig fromConfig$function(ConfigSection section) {
        Key particleType = section.getNonNullIdentifier("particle");
        return new ParticleConfig(
                CraftEngine.instance().platform().getParticleType(particleType),
                section.getNumber("x", ConfigConstants.POSITION_X),
                section.getNumber("y", ConfigConstants.POSITION_Y),
                section.getNumber("z", ConfigConstants.POSITION_Z),
                section.getNumber("count", ConfigConstants.CONSTANT_ONE),
                section.getNumber(OFFSET_X, ConfigConstants.CONSTANT_ZERO),
                section.getNumber(OFFSET_Y, ConfigConstants.CONSTANT_ZERO),
                section.getNumber(OFFSET_Z, ConfigConstants.CONSTANT_ZERO),
                section.getNumber("speed", ConfigConstants.CONSTANT_ZERO),
                Optional.ofNullable(ParticleDataTypes.TYPES.get(particleType)).map(it -> it.apply(section)).orElse(null)
        );
    }

    public static ParticleConfig fromConfig$blockEntity(ConfigSection section) {
        Key particleType = section.getNonNullIdentifier("particle");
        return new ParticleConfig(
                CraftEngine.instance().platform().getParticleType(particleType),
                section.getNumber("x", ConfigConstants.CONSTANT_ZERO),
                section.getNumber("y", ConfigConstants.CONSTANT_ZERO),
                section.getNumber("z", ConfigConstants.CONSTANT_ZERO),
                section.getNumber("count", ConfigConstants.CONSTANT_ONE),
                section.getNumber(OFFSET_X, ConfigConstants.CONSTANT_ZERO),
                section.getNumber(OFFSET_Y, ConfigConstants.CONSTANT_ZERO),
                section.getNumber(OFFSET_Z, ConfigConstants.CONSTANT_ZERO),
                section.getNumber("speed", ConfigConstants.CONSTANT_ZERO),
                Optional.ofNullable(ParticleDataTypes.TYPES.get(particleType)).map(it -> it.apply(section)).orElse(null)
        );
    }

    public ParticleType particleType() {
        return particleType;
    }

    public NumberProvider x() {
        return x;
    }

    public NumberProvider y() {
        return y;
    }

    public NumberProvider z() {
        return z;
    }

    public NumberProvider count() {
        return count;
    }

    public NumberProvider xOffset() {
        return xOffset;
    }

    public NumberProvider yOffset() {
        return yOffset;
    }

    public NumberProvider zOffset() {
        return zOffset;
    }

    public NumberProvider speed() {
        return speed;
    }

    public ParticleData particleData() {
        return particleData;
    }
}
