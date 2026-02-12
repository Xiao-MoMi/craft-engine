package net.momirealms.craftengine.bukkit.world.gen;

import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CraftEngineFeatures {
    public final List<ConditionalFeature> allFeatures;
    public final List<ConditionalFeature> features;
    private final Map<Key, List<Integer>> featureByBiomeCache = new ConcurrentHashMap<>();

    public CraftEngineFeatures(List<ConditionalFeature> allFeatures, List<ConditionalFeature> features) {
        this.allFeatures = allFeatures;
        this.features = features;
    }

    public List<Integer> getFeatureIdsByBiome(final Key biome) {
        return this.featureByBiomeCache.computeIfAbsent(biome, k -> {
            List<Integer> result = new ArrayList<>();
            for (ConditionalFeature feature : this.features) {
                if (feature.isAllowedBiome(biome)) {
                    result.add(feature.id);
                }
            }
            return result;
        });
    }

    public ConditionalFeature getFeatureById(final int id) {
        return this.allFeatures.get(id);
    }
}
