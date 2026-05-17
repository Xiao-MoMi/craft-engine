package net.momirealms.craftengine.core.block.entity.render.tint;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface BlockEntityTintSourceConfigFactory<T extends BlockEntityTintSource> {

    BlockEntityTintSourceConfig<T> create(ConfigSection section);
}
