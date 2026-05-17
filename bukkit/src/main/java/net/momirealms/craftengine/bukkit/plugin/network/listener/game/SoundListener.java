package net.momirealms.craftengine.bukkit.plugin.network.listener.game;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.PacketUtils;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.IdMapProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamEncoderProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;

public final class SoundListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new SoundListener();
    public static final Object SoundEvent$DIRECT_STREAM_CODEC = VersionHelper.isOrAbove1_20_5 ? SoundEventProxy.INSTANCE.getDirectStreamCodec() : null;

    private SoundListener() {}

    @Override
    public void onPacketSend(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int id = buf.readVarInt();
        if (id == 0) {
            Key soundId = buf.readKey();
            Float range = null;
            if (buf.readBoolean()) {
                range = buf.readFloat();
            }
            int source = buf.readVarInt();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            float volume = buf.readFloat();
            float pitch = buf.readFloat();
            long seed = buf.readLong();
            Key mapped = BukkitBlockManager.instance().replaceSoundIfExist(soundId);
            if (mapped != null) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeVarInt(0);
                buf.writeKey(mapped);
                if (range != null) {
                    buf.writeBoolean(true);
                    buf.writeFloat(range);
                } else {
                    buf.writeBoolean(false);
                }
                buf.writeVarInt(source);
                buf.writeInt(x);
                buf.writeInt(y);
                buf.writeInt(z);
                buf.writeFloat(volume);
                buf.writeFloat(pitch);
                buf.writeLong(seed);
            }
        } else {
            Object soundEvent = IdMapProxy.INSTANCE.byId(BuiltInRegistriesProxy.SOUND_EVENT, id - 1);
            if (soundEvent == null) return;
            Key soundId = KeyUtils.identifierToKey(SoundEventProxy.INSTANCE.getLocation(soundEvent));
            int source = buf.readVarInt();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            float volume = buf.readFloat();
            float pitch = buf.readFloat();
            long seed = buf.readLong();
            Key mapped = BukkitBlockManager.instance().replaceSoundIfExist(soundId);
            if (mapped != null) {
                event.setChanged(true);
                buf.clear();
                buf.writeVarInt(event.packetID());
                buf.writeVarInt(0);
                Object newId = KeyUtils.toIdentifier(mapped);
                Object newSoundEvent = SoundEventProxy.INSTANCE.create(newId, SoundEventProxy.INSTANCE.fixedRange(soundEvent));
                if (VersionHelper.isOrAbove1_20_5) {
                    StreamEncoderProxy.INSTANCE.encode(SoundEvent$DIRECT_STREAM_CODEC, buf, newSoundEvent);
                } else {
                    SoundEventProxy.INSTANCE.writeToNetwork(newSoundEvent, PacketUtils.ensureNMSFriendlyByteBuf(buf));
                }
                buf.writeVarInt(source);
                buf.writeInt(x);
                buf.writeInt(y);
                buf.writeInt(z);
                buf.writeFloat(volume);
                buf.writeFloat(pitch);
                buf.writeLong(seed);
            }
        }
    }
}
