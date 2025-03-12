package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.plugin.Reloadable;

import java.util.UUID;

public interface WorldManager extends Reloadable {

    CEWorld getWorld(UUID uuid);

    void delayedInit();
}
