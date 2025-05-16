package net.momirealms.craftengine.core.entity.projectile;

import net.momirealms.craftengine.core.plugin.Manageable;

import java.util.Optional;

public interface ProjectileManager extends Manageable {

    Optional<CustomProjectile> projectileByEntityId(int entityId);
}
