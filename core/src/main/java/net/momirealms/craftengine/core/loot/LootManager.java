package net.momirealms.craftengine.core.loot;

import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;

public interface LootManager extends Manageable {

    ConfigParser[] parsers();

    Optional<VanillaLoot> getBlockLoot(int blockState);

    Optional<VanillaLoot> getEntityLoot(Key entity);

    Optional<Loot> getLoot(Key key);

    LootTableReference createReference(Key key);
}
