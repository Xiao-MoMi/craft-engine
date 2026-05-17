package net.momirealms.craftengine.core.advancement;

import net.momirealms.craftengine.core.plugin.CraftEngine;

public abstract class AbstractAdvancementManager implements AdvancementManager {
    protected final CraftEngine plugin;

    public AbstractAdvancementManager(CraftEngine plugin) {
        this.plugin = plugin;
    }
}
