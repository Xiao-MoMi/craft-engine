package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfig;
import net.momirealms.craftengine.core.entity.culling.CullingData;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record BlockStateAppearance(BlockStateWrapper blockState,
                                   Optional<BlockEntityElementConfig<? extends BlockEntityElement>[]> blockEntityRenderer,
                                   @Nullable CullingData cullingData) {
}
