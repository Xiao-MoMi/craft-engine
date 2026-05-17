package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
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
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSoundPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SoundTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;

import java.util.Optional;

public final class LevelEventListener implements ByteBufferPacketListener {
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
        if (eventId != WorldEvents.BLOCK_BREAK_EFFECT) return;
        BlockPos blockPos = buf.readBlockPos();
        int state = buf.readInt();
        // 移除不透明设置
        if (Config.entityCullingRayTracing()) {
            ClientChunk trackedChunk = user.getTrackedChunk(ChunkPos.asLong(blockPos.x >> 4, blockPos.z >> 4));
            if (trackedChunk != null) {
                trackedChunk.setOccluding(blockPos.x, blockPos.y, blockPos.z, false);
            }
        }
        boolean global = buf.readBoolean();
        int newState = user.clientModEnabled() ? modBlockStateMapper[state] : blockStateMapper[state];
        Object blockState = BlockStateUtils.idToBlockState(state);
        Object soundType = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getSoundType(blockState);
        Object soundEvent = SoundTypeProxy.INSTANCE.getBreakSound(soundType);
        Object rawSoundId = SoundEventProxy.INSTANCE.getLocation(soundEvent);
        if (BlockStateUtils.isVanillaBlock(state)) {
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
    }
}
