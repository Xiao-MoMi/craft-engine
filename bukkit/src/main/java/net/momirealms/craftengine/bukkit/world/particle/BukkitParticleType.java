package net.momirealms.craftengine.bukkit.world.particle;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.particle.ParticleType;
import org.bukkit.Particle;

public final class BukkitParticleType implements ParticleType {
    private final Particle particle;
    private final Key type;

    public BukkitParticleType(Particle particle, Key type) {
        this.particle = particle;
        this.type = type;
    }

    @Override
    public Key type() {
        return this.type;
    }

    @Override
    public Particle platformParticle() {
        return particle;
    }
}
