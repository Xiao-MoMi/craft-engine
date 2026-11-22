package net.momirealms.craftengine.bukkit.world.gen;

import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.ChunkGenerationStage;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ChunkPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelHeightAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ServerLevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.biome.BiomeSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkAccessProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkSectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.PalettedContainerROProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.RandomSupportProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.WorldgenRandomProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.XoroshiroRandomSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.placement.PlacedFeatureProxy;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class InjectedCustomChunkGenerator {
    private final BukkitWorld world;
    private final Object biomeSource;
    private volatile FeatureSnapshot snapshot;

    InjectedCustomChunkGenerator(BukkitWorld world, Object biomeSource) {
        this.world = world;
        this.biomeSource = biomeSource;
    }

    Runnable beginStage(Object chunk, ChunkGenerationStage stage) {
        if (!stage.enabled()) {
            return null;
        }
        return BukkitWorldManager.instance().handleChunkGenerate(this.world, chunkPos(chunk), chunk, stage);
    }

    Runnable beginTerrain(Object chunk) {
        if (!VersionHelper.isOrAbove26_3 || ChunkGenerationStage.NOISE.enabled()) {
            return this.beginStage(chunk, ChunkGenerationStage.NOISE);
        }
        // The merged terrain stage initializes storage once, using the first enabled stage.
        if (ChunkGenerationStage.SURFACE.enabled()) {
            return this.beginStage(chunk, ChunkGenerationStage.SURFACE);
        }
        return this.beginStage(chunk, ChunkGenerationStage.CARVER);
    }

    static void finishStage(Runnable save) {
        if (save != null) {
            save.run();
        }
    }

    static CompletableFuture<?> finishTerrain(CompletableFuture<?> terrain, Runnable save) {
        if (save == null) {
            return terrain;
        }
        return terrain.thenApply(chunk -> {
            save.run();
            return chunk;
        });
    }

    void finishDecoration(Object generator, Object level, Object chunk, Runnable save) {
        CraftEngineFeatures features = this.features(level);
        if (features != null && !features.features.isEmpty()) {
            this.placeFeatures(features, generator, level, chunk);
        }
        finishStage(save);
    }

    private CraftEngineFeatures features(Object level) {
        BukkitWorldManager manager = BukkitWorldManager.instance();
        long reloadTime = manager.lastReloadFeatureTime();
        if (!manager.hasCustomFeatures()) {
            return null;
        }
        FeatureSnapshot cached = this.snapshot;
        if (cached == null || cached.reloadTime() != reloadTime) {
            CraftEngineFeatures features = manager.fetchFeatures(ServerLevelAccessorProxy.INSTANCE.getLevel(level));
            cached = new FeatureSnapshot(reloadTime, features);
            this.snapshot = cached;
        }
        return cached.features();
    }

    private void placeFeatures(CraftEngineFeatures features, Object generator, Object level, Object chunk) {
        ChunkPos pos = chunkPos(chunk);
        int x = pos.x;
        int z = pos.z;
        Set<Object> biomes = collectBiomes(level, x, z);
        biomes.retainAll(BiomeSourceProxy.INSTANCE.possibleBiomes(this.biomeSource));

        Set<Integer> featureIds = new HashSet<>();
        for (Object biome : biomes) {
            featureIds.addAll(features.getFeatureIdsByBiome(KeyUtils.unwrapHolder(biome)));
        }
        if (featureIds.isEmpty()) {
            return;
        }

        int minSection = VersionHelper.isOrAbove1_21_2
                ? LevelHeightAccessorProxy.INSTANCE.getMinSectionY(level)
                : LevelHeightAccessorProxy.INSTANCE.getMinSection(level);
        Object origin = BlockPosProxy.INSTANCE.newInstance(x << 4, minSection << 4, z << 4);
        long seed = RandomSupportProxy.INSTANCE.generateUniqueSeed();
        Object random = WorldgenRandomProxy.INSTANCE.newInstance(XoroshiroRandomSourceProxy.INSTANCE.newInstance(seed));
        for (Integer id : featureIds) {
            PlacedFeatureProxy.INSTANCE.place(features.getFeatureById(id).feature, level, generator, random, origin);
        }
    }

    private static Set<Object> collectBiomes(Object level, int x, int z) {
        Set<Object> biomes = new LinkedHashSet<>();
        Consumer<Object> addBiome = biomes::add;
        for (int chunkZ = z - 1; chunkZ <= z + 1; chunkZ++) {
            for (int chunkX = x - 1; chunkX <= x + 1; chunkX++) {
                Object neighbor = LevelReaderProxy.INSTANCE.getChunk(level, chunkX, chunkZ);
                for (Object section : ChunkAccessProxy.INSTANCE.getSections(neighbor)) {
                    Object palette = LevelChunkSectionProxy.INSTANCE.getBiomes(section);
                    PalettedContainerROProxy.INSTANCE.getAll(palette, addBiome);
                }
            }
        }
        return biomes;
    }

    private static ChunkPos chunkPos(Object chunk) {
        Object pos = ChunkAccessProxy.INSTANCE.getChunkPos(chunk);
        return ChunkPos.of(ChunkPosProxy.INSTANCE.getX(pos), ChunkPosProxy.INSTANCE.getZ(pos));
    }

    private record FeatureSnapshot(long reloadTime, CraftEngineFeatures features) {
    }
}
