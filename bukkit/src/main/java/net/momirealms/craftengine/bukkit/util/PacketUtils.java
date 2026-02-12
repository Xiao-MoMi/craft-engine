package net.momirealms.craftengine.bukkit.util;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.RegistryFriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamDecoderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.codec.StreamEncoderProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetPassengersPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class PacketUtils {
    public static final Object ItemStack$OPTIONAL_STREAM_CODEC = VersionHelper.isOrAbove1_20_5() ? ItemStackProxy.INSTANCE.getOptionalStreamCodec() : null;
    public static final Object ITEM_UNTRUSTED_CODEC = VersionHelper.isOrAbove1_20_5() ? ItemStackProxy.INSTANCE.validatedStreamCodec(ItemStack$OPTIONAL_STREAM_CODEC) : null;

    private PacketUtils() {}

    public static void clientboundSetEntityDataPacket$pack(List<?> trackedValues, ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5()) {
            ClientboundSetEntityDataPacketProxy.INSTANCE.pack$1(trackedValues, ensureNMSFriendlyByteBuf(buf));
        } else {
            ClientboundSetEntityDataPacketProxy.INSTANCE.pack$0(trackedValues, ensureNMSFriendlyByteBuf(buf));
        }
    }

    public static List<Object> clientboundSetEntityDataPacket$unpack(ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5()) {
            return ClientboundSetEntityDataPacketProxy.INSTANCE.unpack$1(ensureNMSFriendlyByteBuf(buf));
        } else {
            return ClientboundSetEntityDataPacketProxy.INSTANCE.unpack$0(ensureNMSFriendlyByteBuf(buf));
        }
    }

    public static ByteBuf ensureNMSFriendlyByteBuf(ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5()) {
            if (RegistryFriendlyByteBufProxy.CLASS.isInstance(buf)) return buf;
            return RegistryFriendlyByteBufProxy.INSTANCE.newInstance(buf, RegistryUtils.getRegistryAccess());
        } else {
            if (FriendlyByteBufProxy.CLASS.isInstance(buf)) return buf;
            return FriendlyByteBufProxy.INSTANCE.newInstance(buf);
        }
    }

    public static Object createClientboundSetPassengersPacket(int vehicle, int... passengers) {
        Object packet = ClientboundSetPassengersPacketProxy.UNSAFE_CONSTRUCTOR.newInstance();
        ClientboundSetPassengersPacketProxy.INSTANCE.setVehicle(packet, vehicle);
        ClientboundSetPassengersPacketProxy.INSTANCE.setPassengers(packet, passengers);
        return packet;
    }

    public static ItemStack readItem(ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5()) {
            return CraftItemStackProxy.INSTANCE.asCraftMirror(StreamDecoderProxy.INSTANCE.decode(ItemStack$OPTIONAL_STREAM_CODEC, ensureNMSFriendlyByteBuf(buf)));
        } else {
            return CraftItemStackProxy.INSTANCE.asCraftMirror(FriendlyByteBufProxy.INSTANCE.readItem(ensureNMSFriendlyByteBuf(buf)));
        }
    }

    public static void writeItem(ByteBuf buf, ItemStack itemStack) {
        if (VersionHelper.isOrAbove1_20_5()) {
            StreamEncoderProxy.INSTANCE.encode(ItemStack$OPTIONAL_STREAM_CODEC, ensureNMSFriendlyByteBuf(buf), CraftItemStackProxy.INSTANCE.unwrap(itemStack));
        } else {
            FriendlyByteBufProxy.INSTANCE.writeItem(ensureNMSFriendlyByteBuf(buf), CraftItemStackProxy.INSTANCE.unwrap(itemStack));
        }
    }

    public static ItemStack readUntrustedItem(ByteBuf buf) {
        if (!VersionHelper.isOrAbove1_20_5()) throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
        return CraftItemStackProxy.INSTANCE.asCraftMirror(StreamDecoderProxy.INSTANCE.decode(ITEM_UNTRUSTED_CODEC, ensureNMSFriendlyByteBuf(buf)));
    }

    public static void writeUntrustedItem(ByteBuf buf, ItemStack itemStack) {
        if (!VersionHelper.isOrAbove1_20_5()) throw new UnsupportedOperationException("This feature is only available on 1.20.5+");
        StreamEncoderProxy.INSTANCE.encode(ITEM_UNTRUSTED_CODEC, ensureNMSFriendlyByteBuf(buf), CraftItemStackProxy.INSTANCE.unwrap(itemStack));
    }
}
