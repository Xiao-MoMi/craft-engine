package net.momirealms.craftengine.bukkit.util;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.RegistryFriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetEntityDataPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundSetPassengersPacketProxy;

import java.util.List;

public final class PacketUtils {
    private PacketUtils() {}

    public static void clientboundSetEntityDataPacket$pack(List<?> trackedValues, ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5()) {
            ClientboundSetEntityDataPacketProxy.INSTANCE.pack$1(trackedValues, wrapByteBuf(buf));
        } else {
            ClientboundSetEntityDataPacketProxy.INSTANCE.pack$0(trackedValues, wrapByteBuf(buf));
        }
    }

    public static List<Object> clientboundSetEntityDataPacket$unpack(ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5()) {
            return ClientboundSetEntityDataPacketProxy.INSTANCE.unpack$1(wrapByteBuf(buf));
        } else {
            return ClientboundSetEntityDataPacketProxy.INSTANCE.unpack$0(wrapByteBuf(buf));
        }
    }

    public static ByteBuf wrapByteBuf(ByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5()) {
            return RegistryFriendlyByteBufProxy.INSTANCE.newInstance(buf, RegistryUtils.getRegistryAccess());
        } else {
            return FriendlyByteBufProxy.INSTANCE.newInstance(buf);
        }
    }

    public static Object createClientboundSetPassengersPacket(int vehicle, int... passengers) {
        Object packet = ClientboundSetPassengersPacketProxy.UNSAFE_CONSTRUCTOR.newInstance();
        ClientboundSetPassengersPacketProxy.INSTANCE.setVehicle(packet, vehicle);
        ClientboundSetPassengersPacketProxy.INSTANCE.setPassengers(packet, passengers);
        return packet;
    }
}
