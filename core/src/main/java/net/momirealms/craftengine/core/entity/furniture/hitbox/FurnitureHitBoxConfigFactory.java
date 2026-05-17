package net.momirealms.craftengine.core.entity.furniture.hitbox;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface FurnitureHitBoxConfigFactory<H extends FurnitureHitBox> {

    FurnitureHitBoxConfig<H> create(ConfigSection section);
}
