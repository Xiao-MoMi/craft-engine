package net.momirealms.craftengine.core.world.particle;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.ConstantNumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.function.Function;

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
    private final boolean directionalStep;
    private final boolean scaleFallCount;

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
        this(particleType, x, y, z, count, xOffset, yOffset, zOffset, speed, particleData, false, false);
    }

    private ParticleConfig(ParticleType particleType, NumberProvider x, NumberProvider y, NumberProvider z,
                           NumberProvider count, NumberProvider xOffset, NumberProvider yOffset, NumberProvider zOffset,
                           NumberProvider speed, ParticleData particleData, boolean directionalStep, boolean scaleFallCount) {
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
        this.directionalStep = directionalStep;
        this.scaleFallCount = scaleFallCount;
    }

    private static final String[] OFFSET_X = ConfigKeys.of("offset_x");
    private static final String[] OFFSET_Y = ConfigKeys.of("offset_y");
    private static final String[] OFFSET_Z = ConfigKeys.of("offset_z");
    private static final String[] STEP_OVERRIDES = ConfigKeys.of("x", "y", "z", "count", "speed", "offset_x", "offset_y", "offset_z");
    private static final NumberProvider BLOCK_CENTER = ConstantNumberProvider.constant(0.5);
    private static final NumberProvider BREAK_COUNT = ConstantNumberProvider.constant(64);
    private static final NumberProvider BREAK_SPREAD = ConstantNumberProvider.constant(0.22);
    private static final NumberProvider BREAK_SPEED = ConstantNumberProvider.constant(0.35);
    private static final NumberProvider ITEM_BREAK_SPEED = ConstantNumberProvider.constant(0.085);
    private static final NumberProvider STEP_HEIGHT = ConstantNumberProvider.constant(0.1);
    private static final NumberProvider STEP_SPREAD = ConstantNumberProvider.constant(0.1);
    private static final NumberProvider STEP_SPEED = ConstantNumberProvider.constant(0.12);
    private static final NumberProvider HIT_SPEED = ConstantNumberProvider.constant(0.02);
    private static final NumberProvider FALL_SPEED = ConstantNumberProvider.constant(0.15);
    private static final NumberProvider ITEM_FALL_SPEED = ConstantNumberProvider.constant(0.055);

    public static ParticleConfig fromConfig$function(ConfigSection section) {
        return fromConfig(section, section.getNonNullIdentifier("particle"),
                ConfigConstants.POSITION_X, ConfigConstants.POSITION_Y, ConfigConstants.POSITION_Z,
                ConfigConstants.CONSTANT_ONE, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO,
                ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO, false, false);
    }

    public static ParticleConfig fromConfig$blockEntity(ConfigSection section) {
        return fromConfig(section, section.getNonNullIdentifier("particle"),
                ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO,
                ConfigConstants.CONSTANT_ONE, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO,
                ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO, false, false);
    }

    public static ParticleConfig fromConfig$blockBreak(ConfigSection section) {
        Key type = section.getNonNullIdentifier("particle");
        NumberProvider speed = ParticleTypes.ITEM.equals(type) ? ITEM_BREAK_SPEED : BREAK_SPEED;
        return fromConfig(section, type, BLOCK_CENTER, BLOCK_CENTER, BLOCK_CENTER,
                BREAK_COUNT, BREAK_SPREAD, BREAK_SPREAD, BREAK_SPREAD, speed, false, false);
    }

    public static ParticleConfig fromConfig$blockStep(ConfigSection section) {
        boolean directionalStep = !section.containsKey(STEP_OVERRIDES);
        return fromConfig(section, section.getNonNullIdentifier("particle"),
                ConfigConstants.CONSTANT_ZERO, STEP_HEIGHT, ConfigConstants.CONSTANT_ZERO,
                ConfigConstants.CONSTANT_ONE, STEP_SPREAD, ConfigConstants.CONSTANT_ZERO, STEP_SPREAD, STEP_SPEED, directionalStep, false);
    }

    public static ParticleConfig fromConfig$blockHit(ConfigSection section) {
        return fromConfig(section, section.getNonNullIdentifier("particle"),
                ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO,
                ConfigConstants.CONSTANT_ONE, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO,
                HIT_SPEED, false, false);
    }

    public static ParticleConfig fromConfig$blockFall(ConfigSection section) {
        Key type = section.getNonNullIdentifier("particle");
        NumberProvider speed = ParticleTypes.ITEM.equals(type) ? ITEM_FALL_SPEED : FALL_SPEED;
        return fromConfig(section, type, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO,
                ConfigConstants.CONSTANT_ONE, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO, ConfigConstants.CONSTANT_ZERO,
                speed, false, !section.containsKey("count"));
    }

    private static ParticleConfig fromConfig(ConfigSection section, Key particleType, NumberProvider x, NumberProvider y, NumberProvider z,
                                             NumberProvider count, NumberProvider xOffset, NumberProvider yOffset,
                                             NumberProvider zOffset, NumberProvider speed, boolean directionalStep, boolean scaleFallCount) {
        ParticleType type = CraftEngine.instance().platform().getParticleType(particleType);
        Function<ConfigSection, ParticleData> dataReader = ParticleDataTypes.TYPES.get(particleType);
        return new ParticleConfig(
                type,
                section.getNumber("x", x),
                section.getNumber("y", y),
                section.getNumber("z", z),
                section.getNumber("count", count),
                section.getNumber(OFFSET_X, xOffset),
                section.getNumber(OFFSET_Y, yOffset),
                section.getNumber(OFFSET_Z, zOffset),
                section.getNumber("speed", speed),
                dataReader == null ? null : dataReader.apply(section),
                directionalStep,
                scaleFallCount
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

    public boolean directionalStep() {
        return this.directionalStep;
    }

    public int fallCount(int fallPower, Context context) {
        if (!this.scaleFallCount) {
            return this.count.getInt(context);
        }
        if (ParticleTypes.ITEM.equals(this.particleType.type())) {
            return 4 + 2 * Math.min(fallPower, 14);
        }
        double density = VersionHelper.isOrAbove1_21_5 ? 0.2F + fallPower / 15.0 : 0.2F + fallPower / 15.0F;
        return (int) (150.0 * Math.min(density, 2.5));
    }
}
