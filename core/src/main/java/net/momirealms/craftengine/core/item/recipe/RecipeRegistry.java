package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.Key;

public interface RecipeRegistry {

    void prepareRegistration();

    Object get(Key id);

    void register(Key id, Object recipe);

    void unregister(Key id);

    void finalizeRegistration();
}
