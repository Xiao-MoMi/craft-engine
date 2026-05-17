package net.momirealms.craftengine.core.item.behavior;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;

public final class EmptyItemBehavior extends ItemBehavior {
    public static final ItemBehaviorFactory<EmptyItemBehavior> FACTORY = new Factory();
    public static final EmptyItemBehavior INSTANCE = new EmptyItemBehavior();

    private EmptyItemBehavior() {}

    private static class Factory implements ItemBehaviorFactory<EmptyItemBehavior> {

        @Override
        public EmptyItemBehavior create(Pack pack, Path path, Key id, ConfigSection section) {
            return INSTANCE;
        }
    }
}
