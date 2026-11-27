package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.chunk.BukkitChunkAccess;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.chunk.Chunk;
import net.momirealms.craftengine.core.world.particle.ParticleData;
import net.momirealms.craftengine.core.world.particle.ParticleType;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.*;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public interface BukkitWorld extends World {

    default org.bukkit.World bukkitWorld() {
        return (org.bukkit.World) platformWorld();
    }

    @Override
    default Chunk getChunkIfLoaded(int x, int z) {
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(this.minecraftWorld());
        Object levelChunk = LevelUtils.getChunkAtIfLoaded(chunkSource, x, z);
        if (levelChunk == null) return null;
        return new BukkitChunkAccess(levelChunk);
    }

    @Override
    default ExistingBlock getBlock(int x, int y, int z) {
        return new BukkitExistingBlock(bukkitWorld().getBlockAt(x, y, z));
    }

    @Override
    default String name() {
        return bukkitWorld().getName();
    }

    @Override
    default Key dimension() {
        Object dimension = LevelProxy.INSTANCE.getDimension(this.minecraftWorld());
        return KeyUtils.identifierToKey(ResourceKeyProxy.INSTANCE.getIdentifier(dimension));
    }

    @Override
    default Path directory() {
        return bukkitWorld().getWorldFolder().toPath();
    }

    @Override
    default UUID uuid() {
        return bukkitWorld().getUID();
    }

    @Override
    default void dropItemNaturally(Position location, Item item) {
        ItemStack itemStack = (ItemStack) item.platformItem();
        if (ItemStackUtils.isEmpty(itemStack)) return;
        if (VersionHelper.isOrAbove1_21_2) {
            bukkitWorld().dropItemNaturally(new Location(null, location.x(), location.y(), location.z()), itemStack);
        } else {
            bukkitWorld().dropItemNaturally(new Location(null, location.x() - 0.5, location.y() - 0.5, location.z() - 0.5), itemStack);
        }
    }

    @Override
    default void dropExp(Position location, int amount) {
        if (amount <= 0) return;
        EntityUtils.spawnEntity(bukkitWorld(), new Location(bukkitWorld(), location.x(), location.y(), location.z()), EntityType.EXPERIENCE_ORB, (e) -> {
            ExperienceOrb orb = (ExperienceOrb) e;
            orb.setExperience(amount);
        });
    }

    @Override
    default void playSound(Position location, Key sound, float volume, float pitch, SoundSource source) {
        bukkitWorld().playSound(new Location(null, location.x(), location.y(), location.z()), sound.toString(), SoundUtils.toBukkit(source), volume, pitch);
    }

    @Override
    default void playBlockSound(Position location, Key sound, float volume, float pitch) {
        bukkitWorld().playSound(new Location(null, location.x(), location.y(), location.z()), sound.toString(), SoundCategory.BLOCKS, volume, pitch);
    }

    @Override
    default void levelEvent(int id, BlockPos pos, int data) {
        LevelAccessorProxy.INSTANCE.levelEvent(this.minecraftWorld(), id, LocationUtils.toBlockPos(pos), data);
    }

    @Override
    default void spawnParticle(Position location, ParticleType particle, int count, double xOffset, double yOffset, double zOffset, double speed, @Nullable ParticleData extraData, @NotNull Context context) {
        ParticleUtils.broadcastParticles(this.minecraftWorld(), particle, location.x(), location.y(), location.z(), count, xOffset, yOffset, zOffset, speed, extraData, context);
    }

    @Override
    default long time() {
        return bukkitWorld().getTime();
    }

    @Override
    default Key getNoiseBiome(int x, int y, int z) {
        return KeyUtils.identifierToKey(LevelReaderProxy.INSTANCE.getNoiseBiome(this.minecraftWorld(), x >> 2, y >> 2, z >> 2));
    }

    @Override
    default void setBlockState(int x, int y, int z, BlockStateWrapper blockState, int flags) {
        Object worldServer = this.minecraftWorld();
        Object blockPos = BlockPosProxy.INSTANCE.newInstance(x, y, z);
        LevelWriterProxy.INSTANCE.setBlock(worldServer, blockPos, blockState.minecraftState(), flags);
    }

    @Override
    default BlockStateWrapper getBlockState(int x, int y, int z) {
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(this.minecraftWorld(), LocationUtils.toBlockPos(x, y, z));
        return BlockStateUtils.toBlockStateWrapper(blockState);
    }

    @Override
    default List<Player> getTrackedBy(ChunkPos pos) {
        Object serverLevel = this.minecraftWorld();
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
        Object chunkMap = ServerChunkCacheProxy.INSTANCE.getChunkMap(chunkSource);
        Object chunkHolder = ChunkMapProxy.INSTANCE.getVisibleChunkIfPresent(chunkMap, pos.longKey);
        if (chunkHolder == null) return Collections.emptyList();
        List<Object> players;
        if (VersionHelper.hasPaperPatch) {
            players = ChunkHolderProxy.INSTANCE.getPlayers(chunkHolder, false);
        } else {
            Object playerProvider = ChunkHolderProxy.INSTANCE.getPlayerProvider(chunkHolder);
            players = ChunkHolderProxy.PlayerProviderProxy.INSTANCE.getPlayers(playerProvider, ChunkPosProxy.INSTANCE.newInstance(pos.x, pos.z), false);
        }
        if (players.isEmpty()) return Collections.emptyList();
        List<Player> tracked = new ArrayList<>(players.size());
        for (Object player : players) {
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(ServerPlayerProxy.INSTANCE.getBukkitEntity(player));
            if (serverPlayer == null) continue;
            tracked.add(serverPlayer);
        }
        return tracked;
    }
}
