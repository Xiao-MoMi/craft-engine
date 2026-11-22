package net.momirealms.craftengine.core.block.setting;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.world.particle.ParticleConfig;
import net.momirealms.craftengine.core.world.particle.ParticleTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record BlockParticles(@Nullable ParticleConfig breakParticle,
                             @Nullable ParticleConfig stepParticle,
                             @Nullable ParticleConfig hitParticle,
                             @Nullable ParticleConfig fallParticle) {
    public static final BlockParticles EMPTY = new BlockParticles(null, null, null, null);

    public BlockParticles(@Nullable ParticleConfig breakParticle, @Nullable ParticleConfig stepParticle) {
        this(breakParticle, stepParticle, null, null);
    }

    public static BlockParticles fromConfig(ConfigValue value) {
        if (!value.is(String.class)) {
            return fromConfig(value.getAsSection());
        }
        ConfigSection itemParticle = ConfigSection.of(value.path(), Map.of(
                "particle", ParticleTypes.ITEM.toString(),
                "item", value.getAsString()
        ));
        return new BlockParticles(
                ParticleConfig.fromConfig$blockBreak(itemParticle),
                ParticleConfig.fromConfig$blockStep(itemParticle),
                ParticleConfig.fromConfig$blockHit(itemParticle),
                ParticleConfig.fromConfig$blockFall(itemParticle)
        );
    }

    public static BlockParticles fromConfig(ConfigSection section) {
        ConfigSection breakSection = section.getSection("break");
        ConfigSection stepSection = section.getSection("step");
        ConfigSection hitSection = section.getSection("hit");
        ConfigSection fallSection = section.getSection("fall");
        return new BlockParticles(
                breakSection == null ? null : ParticleConfig.fromConfig$blockBreak(breakSection),
                stepSection == null ? null : ParticleConfig.fromConfig$blockStep(stepSection),
                hitSection == null ? null : ParticleConfig.fromConfig$blockHit(hitSection),
                fallSection == null ? null : ParticleConfig.fromConfig$blockFall(fallSection)
        );
    }
}
