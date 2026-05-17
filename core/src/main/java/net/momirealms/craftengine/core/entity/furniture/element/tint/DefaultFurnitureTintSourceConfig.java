package net.momirealms.craftengine.core.entity.furniture.element.tint;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public final class DefaultFurnitureTintSourceConfig implements FurnitureTintSourceConfig<DefaultFurnitureTintSource> {
    public static final FurnitureTintSourceConfigFactory<DefaultFurnitureTintSource> FACTORY = new Factory();
    private final List<Key> components;

    private DefaultFurnitureTintSourceConfig(List<Key> components) {
        this.components = components;
    }

    public static DefaultFurnitureTintSourceConfig create(List<Key> components) {
        return new DefaultFurnitureTintSourceConfig(components);
    }

    @Override
    public DefaultFurnitureTintSource create(Furniture furniture) {
        return new DefaultFurnitureTintSource(furniture, this.components);
    }

    public static class Factory implements FurnitureTintSourceConfigFactory<DefaultFurnitureTintSource> {

        @Override
        public FurnitureTintSourceConfig<DefaultFurnitureTintSource> create(ConfigSection section) {
            return new DefaultFurnitureTintSourceConfig(section.getList("components", ConfigValue::getAsIdentifier));
        }
    }
}
