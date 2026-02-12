package net.momirealms.craftengine.bukkit.world.gen;

import net.momirealms.craftengine.core.util.Key;

import java.util.function.Predicate;

public final class ConditionalFeature {
    public final int id;
    public final Object feature;
    private final Predicate<Key> biomes;
    private final Predicate<String> worlds;
    private final Predicate<Key> environments;

    public ConditionalFeature(int id, Object feature, Predicate<Key> biomes, Predicate<String> worlds, Predicate<Key> environments) {
        this.id = id;
        this.feature = feature;
        this.biomes = biomes;
        this.worlds = worlds;
        this.environments = environments;
    }

    public boolean isAllowedBiome(final Key biome) {
        return this.biomes.test(biome);
    }

    public boolean isAllowedWorld(final String world) {
        return this.worlds.test(world);
    }

    public boolean isAllowedEnvironment(final Key environment) {
        return this.environments.test(environment);
    }
}
