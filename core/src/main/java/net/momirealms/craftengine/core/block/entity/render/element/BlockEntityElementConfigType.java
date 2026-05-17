package net.momirealms.craftengine.core.block.entity.render.element;

import net.momirealms.craftengine.core.util.Key;

public record BlockEntityElementConfigType<E extends ConstantBlockEntityElement>(Key id, BlockEntityElementConfigFactory<E> factory) {
}
