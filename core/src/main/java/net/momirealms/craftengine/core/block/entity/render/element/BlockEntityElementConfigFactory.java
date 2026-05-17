package net.momirealms.craftengine.core.block.entity.render.element;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

@FunctionalInterface
public interface BlockEntityElementConfigFactory<E extends ConstantBlockEntityElement> {

    BlockEntityElementConfig<E> create(ConfigSection section);
}
