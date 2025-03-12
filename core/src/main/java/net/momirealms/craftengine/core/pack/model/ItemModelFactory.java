package net.momirealms.craftengine.core.pack.model;

import java.util.Map;

public interface ItemModelFactory {

    ItemModel create(Map<String, Object> arguments);
}
