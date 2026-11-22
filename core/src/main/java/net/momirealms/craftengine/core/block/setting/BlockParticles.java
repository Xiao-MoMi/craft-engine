package net.momirealms.craftengine.core.block.setting;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.particle.ParticleConfig;
import org.jetbrains.annotations.Nullable;

public record BlockParticles(@Nullable ParticleConfig breakParticle, @Nullable ParticleConfig stepParticle) {
    public static final BlockParticles EMPTY = new BlockParticles(null, null);

    public static BlockParticles fromConfig(ConfigSection section) {
        ConfigSection breakSection = section.getSection("break");
        ConfigSection stepSection = section.getSection("step");
        return new BlockParticles(
                breakSection == null ? null : ParticleConfig.fromConfig$blockBreak(breakSection),
                stepSection == null ? null : ParticleConfig.fromConfig$blockStep(stepSection)
        );
    }
}
