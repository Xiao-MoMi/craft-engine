package net.momirealms.craftengine.bukkit.world.particle;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.particle.ParticleType;

public final class BukkitParticleType implements ParticleType {
    private final Object particle;
    private final Key type;

    public BukkitParticleType(Object particle, Key type) {
        this.particle = particle;
        this.type = type;
    }

    @Override
    public Key type() {
        return this.type;
    }

    @Override
    public Object platformParticle() {
        return this.particle;
    }
}
