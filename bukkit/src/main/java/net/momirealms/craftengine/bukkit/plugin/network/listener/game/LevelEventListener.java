package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.ParticleUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;
import net.momirealms.craftengine.core.world.particle.ParticleConfig;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSoundPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SoundTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import org.bukkit.Location;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class LevelEventListener implements ByteBufferPacketListener {
    private static final Cache<WorldBlockPos, Integer> IGNORED_EVENTS = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(1))
            .build();
    private static final Cache<PlayerBreakEffect, AtomicInteger> PLAYER_BREAK_EFFECTS = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(1))
            .build();
    private final int[] blockStateMapper;
    private final int[] modBlockStateMapper;

    public LevelEventListener(int[] blockStateMapper, int[] modBlockStateMapper) {
        this.blockStateMapper = blockStateMapper;
        this.modBlockStateMapper = modBlockStateMapper;
    }

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int eventId = buf.readInt();
        BlockPos blockPos = buf.readBlockPos();
        if (eventId == WorldEvents.BLOCK_BREAK_EFFECT) {
            int state = buf.readInt();
            // 移除不透明设置
            if (Config.entityCullingRayTracing()) {
                ClientChunk trackedChunk = user.getTrackedChunk(ChunkPos.asLong(blockPos.x >> 4, blockPos.z >> 4));
                if (trackedChunk != null) {
                    trackedChunk.setOccluding(blockPos.x, blockPos.y, blockPos.z, false);
                }
            }
            boolean global = buf.readBoolean();
            int newState = user.clientCustomBlockEnabled() ? modBlockStateMapper[state] : blockStateMapper[state];
            Object blockState = BlockStateUtils.idToBlockState(state);
            Object soundType = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(blockState);
            Object soundEvent = SoundTypeProxy.INSTANCE.getBreakSound(soundType);
            Object rawSoundId = SoundEventProxy.INSTANCE.getLocation(soundEvent);
            if (BlockStateUtils.isVanillaBlock(state)) {
                if (state != 0) {
                    if (BukkitBlockManager.instance().isBreakSoundMissing(rawSoundId)) {
                        Key mappedSoundId = BukkitBlockManager.instance().replaceSoundIfExist(KeyUtils.identifierToKey(rawSoundId));
                        if (mappedSoundId != null) {
                            Object packet = ClientboundSoundPacketProxy.INSTANCE.newInstance(
                                    HolderProxy.INSTANCE.direct(SoundEventProxy.INSTANCE.create(KeyUtils.toIdentifier(mappedSoundId), Optional.empty())),
                                    SoundSourceProxy.BLOCKS,
                                    blockPos.x() + 0.5, blockPos.y() + 0.5, blockPos.z() + 0.5, 1f, 0.8F,
                                    RandomUtils.generateRandomLong()
                            );
                            user.sendPacket(packet, true);
                        }
                    }
                }
            } else {
                Key soundId = KeyUtils.identifierToKey(rawSoundId);
                Key mappedSoundId = BukkitBlockManager.instance().replaceSoundIfExist(soundId);
                Object finalSoundId = KeyUtils.toIdentifier(mappedSoundId == null ? soundId : mappedSoundId);
                Object packet = ClientboundSoundPacketProxy.INSTANCE.newInstance(
                        HolderProxy.INSTANCE.direct(SoundEventProxy.INSTANCE.create(finalSoundId, Optional.empty())),
                        SoundSourceProxy.BLOCKS,
                        blockPos.x() + 0.5, blockPos.y() + 0.5, blockPos.z() + 0.5, 1f, 0.8F,
                        RandomUtils.generateRandomLong()
                );
                user.sendPacket(packet, true);
                ImmutableBlockState customState = BlockStateUtils.getNullableCustomBlockState(blockState);
                if (customState != null) {
                    ParticleConfig particle = customState.settings().particles().breakParticle();
                    if (particle != null) {
                        PlayerBreakEffect playerBreak = new PlayerBreakEffect(user.clientSideWorld().uuid(), blockPos, state, user.uuid());
                        AtomicInteger pending = PLAYER_BREAK_EFFECTS.getIfPresent(playerBreak);
                        if (pending != null && pending.getAndUpdate(count -> count > 0 ? count - 1 : 0) > 0) {
                            event.setCancelled(true);
                            return;
                        }
                        Object particles = ParticleUtils.createParticlePacket(particle, blockPos.x(), blockPos.y(), blockPos.z());
                        if (particles != null) {
                            user.sendPacket(particles, true);
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
            if (newState == state) {
                return;
            }
            event.setChanged(true);
            buf.clear();
            buf.writeVarInt(event.packetID());
            buf.writeInt(eventId);
            buf.writeBlockPos(blockPos);
            buf.writeInt(newState);
            buf.writeBoolean(global);
        } else if (eventId == WorldEvents.BLAZE_SHOOTS) {
            WorldBlockPos worldBlockPos = new WorldBlockPos(user.clientSideWorld().uuid(), blockPos);
            Integer eventsIfPresent = IGNORED_EVENTS.getIfPresent(worldBlockPos);
            if (eventsIfPresent != null && eventsIfPresent == eventId) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public static void addTempIgnoredEvent(Location location, int eventId) {
        IGNORED_EVENTS.put(WorldBlockPos.fromLocation(location), eventId);
    }

    public static void markPlayerBreakEffect(UUID worldId, BlockPos pos, int stateId, UUID viewerId) {
        PLAYER_BREAK_EFFECTS.get(new PlayerBreakEffect(worldId, pos, stateId, viewerId), ignored -> new AtomicInteger()).incrementAndGet();
    }

    private record PlayerBreakEffect(UUID worldId, BlockPos pos, int stateId, UUID viewerId) {
    }

    public record WorldBlockPos(UUID uuid, BlockPos blockPos) {

        public static WorldBlockPos fromLocation(Location location) {
            return new WorldBlockPos(location.getWorld().getUID(), new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        }
    }
}
