package net.momirealms.craftengine.bukkit.plugin.network.packet;

import net.momirealms.craftengine.bukkit.plugin.network.handler.FurniturePacketHandler;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.mod.ClientCustomPacket;
import net.momirealms.craftengine.core.plugin.network.mod.CustomPackets;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public record ClientboundFurnitureUpdatePacket(int entityId) implements ClientCustomPacket {
    public static final Key ID = Key.ce("furniture_update");
    public static final NetworkCodec<FriendlyByteBuf, ClientboundFurnitureUpdatePacket> CODEC = ClientCustomPacket.codec(
            (packet, buf) -> buf.writeVarInt(packet.entityId),
            buf -> new ClientboundFurnitureUpdatePacket(buf.readVarInt())
    );

    public static void init() {
        CustomPackets.registerClientbound(ID, CODEC, CustomPackets.ALWAYS_ALLOWED, true);
    }

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ClientboundFurnitureUpdatePacket> codec() {
        return CODEC;
    }

    @Override
    public void handle(NetWorkUser user, ByteBufPacketEvent event) {
        event.setCancelled(true);
        if (user.entityViews().get(this.entityId) instanceof FurniturePacketHandler handler) {
            handler.synchronize((Player) user);
        }
    }
}
