package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.particle.ParticleData;
import net.momirealms.craftengine.core.world.particle.ParticleType;
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

public class BukkitWorld implements World {
    private final WeakReference<org.bukkit.World> world;
    private final UUID uuid;
    private CEWorld ceWorld;
    private WorldHeight worldHeight;

    public BukkitWorld(@NotNull org.bukkit.World world) {
        this.world = new WeakReference<>(world);
        this.uuid = world.getUID();
    }

    @Override
    public org.bukkit.World platformWorld() {
        return this.world.get();
    }

    @Override
    public Object serverWorld() {
        return FastNMS.INSTANCE.field$CraftWorld$ServerLevel(platformWorld());
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
    public BlockStateWrapper getBlockState(int x, int y, int z) {
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(this.serverWorld(), LocationUtils.toBlockPos(x, y, z));
        return BlockStateUtils.toBlockStateWrapper(blockState);
    }

    @Override
    public ExistingBlock getBlock(int x, int y, int z) {
        return new BukkitExistingBlock(platformWorld().getBlockAt(x, y, z));
    }

    @Override
    public String name() {
        return platformWorld().getName();
    }

    @Override
    public Path directory() {
        return platformWorld().getWorldFolder().toPath();
    }

    @Override
    public UUID uuid() {
        return platformWorld().getUID();
    }

    @Override
    public void dropItemNaturally(Position location, Item<?> item) {
        ItemStack itemStack = (ItemStack) item.getItem();
        if (ItemStackUtils.isEmpty(itemStack)) return;
        if (VersionHelper.isOrAbove1_21_2()) {
            platformWorld().dropItemNaturally(new Location(null, location.x(), location.y(), location.z()), (ItemStack) item.getItem());
        } else {
            platformWorld().dropItemNaturally(new Location(null, location.x() - 0.5, location.y() - 0.5, location.z() - 0.5), (ItemStack) item.getItem());
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
        Object worldServer = serverWorld();
        Object blockPos = FastNMS.INSTANCE.constructor$BlockPos(x, y, z);
        FastNMS.INSTANCE.method$LevelWriter$setBlock(worldServer, blockPos, blockState.literalObject(), flags);
    }

    @Override
    public void levelEvent(int id, BlockPos pos, int data) {
        FastNMS.INSTANCE.method$LevelAccessor$levelEvent(serverWorld(), id, LocationUtils.toBlockPos(pos), data);
    }

    @Override
    public CEWorld storageWorld() {
        if (this.ceWorld == null) {
            this.ceWorld = BukkitWorldManager.instance().getWorld(uuid());
        }
        return this.ceWorld;
    }

    @Override
    public Key getNoiseBiome(int x, int y, int z) {
        return KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.method$LevelReader$getNoiseBiome(serverWorld(), x >> 2, y >> 2, z >> 2));
    }

    @Override
    public List<Player> getTrackedBy(ChunkPos pos) {
        Object serverLevel = serverWorld();
        Object chunkSource = FastNMS.INSTANCE.method$ServerLevel$getChunkSource(serverLevel);
        Object chunkHolder = FastNMS.INSTANCE.method$ServerChunkCache$getVisibleChunkIfPresent(chunkSource, pos.longKey);
        if (chunkHolder == null) return Collections.emptyList();
        List<Object> players = FastNMS.INSTANCE.method$ChunkHolder$getPlayers(chunkHolder);
        if (players.isEmpty()) return Collections.emptyList();
        List<Player> tracked = new ArrayList<>(players.size());
        for (Object player : players) {
            tracked.add(BukkitAdaptors.adapt(FastNMS.INSTANCE.method$ServerPlayer$getBukkitEntity(player)));
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
