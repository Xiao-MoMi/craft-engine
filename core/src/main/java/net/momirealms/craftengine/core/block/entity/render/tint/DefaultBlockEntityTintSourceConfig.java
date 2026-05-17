package net.momirealms.craftengine.core.block.entity.render.tint;

import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.TintSource;
import net.momirealms.craftengine.core.world.chunk.CEChunk;

import java.util.List;

public final class DefaultBlockEntityTintSourceConfig implements BlockEntityTintSourceConfig<DefaultBlockEntityTintSource> {
    public static final BlockEntityTintSourceConfigFactory<DefaultBlockEntityTintSource> FACTORY = new Factory();
    private final List<Key> components;
    private final int tintIndex;

    private DefaultBlockEntityTintSourceConfig(List<Key> components, int tintIndex) {
        this.components = components;
        this.tintIndex = tintIndex;
    }

    public static DefaultBlockEntityTintSourceConfig create(List<Key> components, int tintIndex) {
        return new DefaultBlockEntityTintSourceConfig(components, tintIndex);
    }

    @Override
    public DefaultBlockEntityTintSource create(CEChunk chunk, BlockPos pos) {
        BlockEntity blockEntity = chunk.getBlockEntity(pos, false);
        if (blockEntity != null) {
            TintSource tintSource = blockEntity.controller.get(TintSource.class, this.tintIndex);
            if (tintSource != null) {
                return new DefaultBlockEntityTintSource(tintSource, this.components);
            }
        }
        return null;
    }

    public static class Factory implements BlockEntityTintSourceConfigFactory<DefaultBlockEntityTintSource> {

        @Override
        public BlockEntityTintSourceConfig<DefaultBlockEntityTintSource> create(ConfigSection section) {
            return new DefaultBlockEntityTintSourceConfig(
                    section.getList("components", ConfigValue::getAsIdentifier),
                    section.getInt("index", 0)
            );
        }
    }
}
