package net.momirealms.craftengine.bukkit.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.event.NMSPacketEvent;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.RegistryFriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.ByteBufCodecsProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamDecoderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamEncoderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.BundlePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ClientboundCustomPayloadPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ServerboundCustomPayloadPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.custom.DiscardedPayloadProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundBundlePacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetPassengersPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;

import java.util.ArrayList;
import java.util.List;

public final class PacketUtils {
    public static final Object ItemStack$OPTIONAL_STREAM_CODEC = VersionHelper.isOrAbove1_20_5 ? ItemStackProxy.INSTANCE.getOptionalStreamCodec() : null;
    public static final Object ItemStackTemplate$STREAM_CODEC = VersionHelper.isOrAbove26_1 ? ItemStackTemplateProxy.INSTANCE.getStreamCodec() : null;
    public static final Object UNTRUSTED_ITEM_CODEC = VersionHelper.isOrAbove1_20_5 ? createUntrustedItemCodec() : null;

    private PacketUtils() {}

    private static Object createUntrustedItemCodec() {
        Object optionalItemCodec = VersionHelper.isOrAbove1_21_5
                ? ItemStackProxy.INSTANCE.getOptionalUntrustedStreamCodec()
                : ItemStackProxy.INSTANCE.getOptionalStreamCodec();
        Object untrustedItemCodec = ItemStackProxy.INSTANCE.validatedStreamCodec(optionalItemCodec);
        // 1.21.4+ Paper 需要限制组件反序列化深度，等价于 codec.apply(ByteBufCodecs::trackDepth)
        if (VersionHelper.isOrAbove1_21_4 && VersionHelper.hasPaperPatch) {
            untrustedItemCodec = ByteBufCodecsProxy.INSTANCE.trackDepth(untrustedItemCodec);
        }
        return untrustedItemCodec;
    }

    public static void clientboundSetEntityDataPacket$pack(List<?> trackedValues, ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5) {
            ClientboundSetEntityDataPacketProxy.INSTANCE.pack$1(trackedValues, ensureNMSFriendlyByteBuf(buf));
        } else {
            ClientboundSetEntityDataPacketProxy.INSTANCE.pack$0(trackedValues, ensureNMSFriendlyByteBuf(buf));
        }
    }

    public static List<Object> clientboundSetEntityDataPacket$unpack(ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5) {
            return ClientboundSetEntityDataPacketProxy.INSTANCE.unpack$1(ensureNMSFriendlyByteBuf(buf));
        } else {
            return ClientboundSetEntityDataPacketProxy.INSTANCE.unpack$0(ensureNMSFriendlyByteBuf(buf));
        }
    }

    public static ByteBuf ensureNMSFriendlyByteBuf(ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5) {
            if (RegistryFriendlyByteBufProxy.CLASS.isInstance(buf)) return buf;
            return RegistryFriendlyByteBufProxy.INSTANCE.newInstance(buf, RegistryUtils.getRegistryAccess());
        } else {
            if (FriendlyByteBufProxy.CLASS.isInstance(buf)) return buf;
            return FriendlyByteBufProxy.INSTANCE.newInstance(buf);
        }
    }

    public static Object createClientboundCustomPayloadPacket(Key channel, byte[] data) {
        Object id = KeyUtils.toIdentifier(channel);
        if (!VersionHelper.isOrAbove1_20_2) {
            // 1.20–1.20.1：包直接保存频道与正文，不包含协议包 ID。
            return ClientboundCustomPayloadPacketProxy.INSTANCE.newInstance(id, FriendlyByteBufProxy.INSTANCE.newInstance(Unpooled.wrappedBuffer(data)));
        }
        Object payload;
        if (VersionHelper.isOrAbove1_20_5) {
            // 1.20.5+：Bukkit 的 DiscardedPayload 保留正文，Paper 补丁使用 byte[]。
            payload = DiscardedPayloadProxy.CONSTRUCTOR.newInstance(id, DiscardedPayloadProxy.PAPER_PATCH ? data : Unpooled.wrappedBuffer(data));
        } else {
            // 1.20.2–1.20.4：DiscardedPayload 不保存正文；UnknownPayload 实现同一接口且可写出原始数据。
            payload = ServerboundCustomPayloadPacketProxy.UnknownPayloadProxy.CONSTRUCTOR.newInstance(id, ServerboundCustomPayloadPacketProxy.UnknownPayloadProxy.PAPER_PATCH ? data : Unpooled.wrappedBuffer(data));
        }
        return ClientboundCustomPayloadPacketProxy.INSTANCE.newInstance(payload);
    }

    public static Object createClientboundSetPassengersPacket(int vehicle, int... passengers) {
        Object packet = ClientboundSetPassengersPacketProxy.UNSAFE_CONSTRUCTOR.newInstance();
        ClientboundSetPassengersPacketProxy.INSTANCE.setVehicle(packet, vehicle);
        ClientboundSetPassengersPacketProxy.INSTANCE.setPassengers(packet, passengers);
        return packet;
    }

    public static Item readItem(ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5) {
            return ItemStackUtils.wrap(StreamDecoderProxy.INSTANCE.decode(ItemStack$OPTIONAL_STREAM_CODEC, ensureNMSFriendlyByteBuf(buf)));
        } else {
            return ItemStackUtils.wrap(FriendlyByteBufProxy.INSTANCE.readItem(ensureNMSFriendlyByteBuf(buf)));
        }
    }

    public static void writeItem(ByteBuf buf, Item item) {
        if (VersionHelper.isOrAbove1_20_5) {
            StreamEncoderProxy.INSTANCE.encode(ItemStack$OPTIONAL_STREAM_CODEC, ensureNMSFriendlyByteBuf(buf), item.minecraftItem());
        } else {
            FriendlyByteBufProxy.INSTANCE.writeItem(ensureNMSFriendlyByteBuf(buf), item.minecraftItem());
        }
    }

    public static Item readItemTemplate(ByteBuf buf) {
        if (!VersionHelper.isOrAbove26_1) throw new UnsupportedOperationException("This feature is only available on 26.1+");
        Object template = StreamDecoderProxy.INSTANCE.decode(ItemStackTemplate$STREAM_CODEC, ensureNMSFriendlyByteBuf(buf));
        return ItemStackUtils.wrap(ItemStackTemplateProxy.INSTANCE.create(template));
    }

    public static void writeItemTemplate(ByteBuf buf, Item item) {
        if (!VersionHelper.isOrAbove26_1) throw new UnsupportedOperationException("This feature is only available on 26.1+");
        StreamEncoderProxy.INSTANCE.encode(ItemStackTemplate$STREAM_CODEC, ensureNMSFriendlyByteBuf(buf), ItemStackTemplateProxy.INSTANCE.fromNonEmptyStack(item.minecraftItem()));
    }

    public static Item readUntrustedItem(ByteBuf buf) {
        if (!VersionHelper.isOrAbove1_20_5) throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
        return ItemStackUtils.wrap(StreamDecoderProxy.INSTANCE.decode(UNTRUSTED_ITEM_CODEC, ensureNMSFriendlyByteBuf(buf)));
    }

    public static void writeUntrustedItem(ByteBuf buf, Item item) {
        if (!VersionHelper.isOrAbove1_20_5) throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
        StreamEncoderProxy.INSTANCE.encode(UNTRUSTED_ITEM_CODEC, ensureNMSFriendlyByteBuf(buf), item.minecraftItem());
    }

    public static void replacePacket(NMSPacketEvent event, Object oldPacket, Object newPacket) {
        Object packet = event.optionalNewPacket();
        if (packet == null) packet = event.getPacket();
        if (ClientboundBundlePacketProxy.CLASS.isInstance(packet)) {
            Iterable<Object> packets = BundlePacketProxy.INSTANCE.getPackets(packet);
            List<Object> newPackets = new ObjectArrayList<>(2);
            for (Object packetInBundle : packets) {
                if (packetInBundle == oldPacket) {
                    newPackets.add(newPacket);
                } else {
                    newPackets.add(packetInBundle);
                }
            }
            event.replacePacket(ClientboundBundlePacketProxy.INSTANCE.newInstance(newPackets));
        } else {
            event.replacePacket(newPacket);
        }
    }
}
