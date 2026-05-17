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
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.*;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelReaderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BukkitWorld implements World {
    private final WeakReference<org.bukkit.World> bukkitWorld;
    private final WeakReference<Object> minecraftWorld;
    private final UUID uuid;
    private final String worldName;
    private final Path worldFolder;
    private CEWorld ceWorld;
    private WorldHeight worldHeight;

    public BukkitWorld(@NotNull org.bukkit.World bukkitWorld) {
        this.bukkitWorld = new WeakReference<>(bukkitWorld);
        this.minecraftWorld = new WeakReference<>(CraftWorldProxy.INSTANCE.getWorld(bukkitWorld));
        this.uuid = bukkitWorld.getUID();
        this.worldName = bukkitWorld.getName();
        this.worldFolder = bukkitWorld.getWorldFolder().toPath(); // 低版本没有直接获取path
    }

    @Override
    public org.bukkit.World platformWorld() {
        return this.bukkitWorld.get();
    }

    @Override
    public Object minecraftWorld() {
        return this.minecraftWorld.get();
    }

    @Override
    public CEWorld storageWorld() {
        if (this.ceWorld == null) {
            this.ceWorld = BukkitWorldManager.instance().getWorld(uuid());
        }
        return this.ceWorld;
    }

    @Override
    public WorldHeight worldHeight() {
        if (this.worldHeight == null) {
            org.bukkit.World bWorld = platformWorld();
            this.worldHeight = WorldHeight.create(bWorld.getMinHeight(), bWorld.getMaxHeight() - bWorld.getMinHeight());
        }
        return this.worldHeight;
    }

    @Override
    public Chunk getChunkIfLoaded(int x, int z) {
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(this.minecraftWorld());
        Object levelChunk;
        if (VersionHelper.isOrAbove1_21) {
            levelChunk = ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedImmediately(chunkSource, x, z);
        } else {
            levelChunk = ServerChunkCacheProxy.INSTANCE.getChunkAtIfLoadedMainThread(chunkSource, x, z);
        }
        if (levelChunk == null) return null;
        return new BukkitChunkAccess(levelChunk);
    }

    @Override
    public BlockStateWrapper getBlockState(int x, int y, int z) {
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(this.minecraftWorld(), LocationUtils.toBlockPos(x, y, z));
        return BlockStateUtils.toBlockStateWrapper(blockState);
    }

    @Override
    public ExistingBlock getBlock(int x, int y, int z) {
        return new BukkitExistingBlock(platformWorld().getBlockAt(x, y, z));
    }

    @Override
    public String name() {
        return this.worldName;
    }

    @Override
    public Path directory() {
        return this.worldFolder;
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public void dropItemNaturally(Position location, Item item) {
        ItemStack itemStack = ItemStackProxy.INSTANCE.getBukkitStack(item.minecraftItem());
        if (ItemStackUtils.isEmpty(itemStack)) return;
        if (VersionHelper.isOrAbove1_21_2) {
            platformWorld().dropItemNaturally(new Location(null, location.x(), location.y(), location.z()), itemStack);
        } else {
            platformWorld().dropItemNaturally(new Location(null, location.x() - 0.5, location.y() - 0.5, location.z() - 0.5), itemStack);
        }
    }

    @Override
    public void dropExp(Position location, int amount) {
        if (amount <= 0) return;
        EntityUtils.spawnEntity(platformWorld(), new Location(platformWorld(), location.x(), location.y(), location.z()), EntityType.EXPERIENCE_ORB, (e) -> {
            ExperienceOrb orb = (ExperienceOrb) e;
            orb.setExperience(amount);
        });
    }

    @Override
    public void playSound(Position location, Key sound, float volume, float pitch, SoundSource source) {
        platformWorld().playSound(new Location(null, location.x(), location.y(), location.z()), sound.toString(), SoundUtils.toBukkit(source), volume, pitch);
    }

    @Override
    public void playBlockSound(Position location, Key sound, float volume, float pitch) {
        platformWorld().playSound(new Location(null, location.x(), location.y(), location.z()), sound.toString(), SoundCategory.BLOCKS, volume, pitch);
    }

    @Override
    public void spawnParticle(Position location, ParticleType particle, int count, double xOffset, double yOffset, double zOffset, double speed, @Nullable ParticleData extraData, @NotNull Context context) {
        Particle particleType = (Particle) particle.platformParticle();
        if (particleType == null) return;
        org.bukkit.World platformWorld = platformWorld();
        platformWorld.spawnParticle(particleType, location.x(), location.y(), location.z(), count, xOffset, yOffset, zOffset, speed, extraData == null ? null : ParticleUtils.toBukkitParticleData(extraData, context, platformWorld, location.x(), location.y(), location.z()));
    }

    @Override
    public long time() {
        return platformWorld().getTime();
    }

    @Override
    public void setBlockState(int x, int y, int z, BlockStateWrapper blockState, int flags) {
        Object worldServer = this.minecraftWorld();
        Object blockPos = BlockPosProxy.INSTANCE.newInstance(x, y, z);
        LevelWriterProxy.INSTANCE.setBlock(worldServer, blockPos, blockState.minecraftState(), flags);
    }

    @Override
    public void levelEvent(int id, BlockPos pos, int data) {
        LevelAccessorProxy.INSTANCE.levelEvent(this.minecraftWorld(), id, LocationUtils.toBlockPos(pos), data);
    }

    @Override
    public Key getNoiseBiome(int x, int y, int z) {
        return KeyUtils.identifierToKey(LevelReaderProxy.INSTANCE.getNoiseBiome(this.minecraftWorld(), x >> 2, y >> 2, z >> 2));
    }

    @Override
    public List<Player> getTrackedBy(ChunkPos pos) {
        Object serverLevel = this.minecraftWorld();
        Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
        Object chunkMap = ServerChunkCacheProxy.INSTANCE.getChunkMap(chunkSource);
        Object chunkHolder = ChunkMapProxy.INSTANCE.getVisibleChunkIfPresent(chunkMap, pos.longKey);
        if (chunkHolder == null) return Collections.emptyList();
        List<Object> players = ChunkHolderProxy.INSTANCE.getPlayers(chunkHolder, false);
        if (players.isEmpty()) return Collections.emptyList();
        List<Player> tracked = new ArrayList<>(players.size());
        for (Object player : players) {
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(ServerPlayerProxy.INSTANCE.getBukkitEntity(player));
            if (serverPlayer == null) continue;
            tracked.add(serverPlayer);
        }
        return tracked;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BukkitWorld that)) return false;
        return this.uuid.equals(that.uuid());
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }
}
