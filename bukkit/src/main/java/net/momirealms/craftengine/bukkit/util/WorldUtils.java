package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftChunkProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WorldUtils {
    private WorldUtils() {}

    @Nullable
    public static Object getMinecraftChunk(@NotNull Chunk bukkitChunk) {
        Object worldServer = CraftChunkProxy.INSTANCE.getWorld(bukkitChunk);
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(worldServer);

        if (VersionHelper.isOrAbove1_21) {
            return ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedImmediately(chunkSource, bukkitChunk.getX(), bukkitChunk.getZ());
        } else {
            return ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedMainThread(chunkSource, bukkitChunk.getX(), bukkitChunk.getZ());
        }
    }

    public static Object getEntityLookup(World world) {
        Object serverLevel = CraftWorldProxy.INSTANCE.getWorld(world);
        Object entityLookup;
        if (VersionHelper.isOrAbove1_21) {
            entityLookup = LevelProxy.INSTANCE.moonrise$getEntityLookup(serverLevel);
        } else {
            entityLookup = ServerLevelProxy.INSTANCE.getEntityLookup(serverLevel);
        }
        return entityLookup;
    }
}
