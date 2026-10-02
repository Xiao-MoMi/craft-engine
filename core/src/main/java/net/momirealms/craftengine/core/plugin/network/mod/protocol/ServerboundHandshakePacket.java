package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.ProtocolVersion;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodecs;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.mod.CustomPackets;
import net.momirealms.craftengine.core.plugin.network.mod.ServerCustomPacket;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IntIdentityList;
import net.momirealms.craftengine.core.util.Key;

public record ServerboundHandshakePacket(int protocolVersion, int blockListSize) implements ServerCustomPacket {
    public static final Key ID = Key.ce("handshake");
    public static final NetworkCodec<FriendlyByteBuf, ServerboundHandshakePacket> CODEC = ServerCustomPacket.codec(
            (packet, buf) -> {
                NetworkCodecs.VAR_INTEGER.encode(buf, packet.protocolVersion);
                NetworkCodecs.VAR_INTEGER.encode(buf, packet.blockListSize);
            },
            buf -> new ServerboundHandshakePacket(
                    NetworkCodecs.VAR_INTEGER.decode(buf),
                    NetworkCodecs.VAR_INTEGER.decode(buf)
            )
    );

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ServerboundHandshakePacket> codec() {
        return CODEC;
    }

    @Override
    public void handle(NetWorkUser user, ByteBufPacketEvent event) {
        user.setClientModProtocol(this.protocolVersion);
        user.setClientBlockList(new IntIdentityList(this.blockListSize));
        CustomPackets.checkProtocolVersion(user);
        // 1.20.1 或更低版本没有配置阶段所以在这里处理
        if (user.hasClientMod() && !user.protocolVersion().isVersionNewerThan(ProtocolVersion.V1_20_2)
                && user instanceof Player player && player.platformPlayer() != null) {
            user.sendCustomPackets(ClientboundCreativeModeTabItemsPacket.create(player));
        }
    }
}
