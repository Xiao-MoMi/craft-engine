package net.momirealms.craftengine.core.block.behavior;

import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import org.jetbrains.annotations.Nullable;

public interface BlockBehaviorFactory<T extends BlockBehavior> {

    T create(BlockDefinition block, ConfigSection section);

    @SuppressWarnings("unchecked")
    static <A extends Comparable<A>> Property<A> getProperty(String path, BlockDefinition block, String name, Class<A> valueClass) {
        Property<?> property = block.getProperty(name);
        if (property == null) {
            throw new KnownResourceException("resource.block.behavior.missing_property", path, name);
        }
        if (property.valueClass() != valueClass) {
            throw new KnownResourceException("resource.block.behavior.property_type_mismatch", path, valueClass.getSimpleName(), property.valueClass().getSimpleName());
        }
        return (Property<A>) property;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    static <A extends Comparable<A>> Property<A> getOptionalProperty(BlockDefinition block, String name, Class<A> valueClass) {
        Property<?> property = block.getProperty(name);
        if (property == null) {
            return null;
        }
        if (property.valueClass() != valueClass) {
            return null;
        }
        return (Property<A>) property;
    }
}
