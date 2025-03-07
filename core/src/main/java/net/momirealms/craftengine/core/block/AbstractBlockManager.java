package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.pack.model.generator.AbstractModelGenerator;
import net.momirealms.craftengine.core.plugin.CraftEngine;

public abstract class AbstractBlockManager extends AbstractModelGenerator implements BlockManager {

    public AbstractBlockManager(CraftEngine plugin) {
        super(plugin);
    }
}
