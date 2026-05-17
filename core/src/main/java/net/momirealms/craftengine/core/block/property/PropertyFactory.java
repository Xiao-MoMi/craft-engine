package net.momirealms.craftengine.core.block.property;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;

public interface PropertyFactory<T extends Comparable<T>> {

    Property<T> create(String name, ConfigSection section);
}
