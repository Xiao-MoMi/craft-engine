package net.momirealms.craftengine.core.block.entity.render.tint;

import net.momirealms.craftengine.core.util.Key;

public record BlockEntityTintSourceType<T extends BlockEntityTintSource>(Key id, BlockEntityTintSourceConfigFactory<T> factory) {
}
