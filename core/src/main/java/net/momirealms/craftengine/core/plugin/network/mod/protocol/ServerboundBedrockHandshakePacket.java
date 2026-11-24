package net.momirealms.craftengine.core.plugin.network.mod.protocol;

import net.momirealms.craftengine.bedrock.network.BedrockProtocol;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.network.ClientboundItemRewriter;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.mod.ServerCustomPacket;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerAdapter;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

public record ServerboundBedrockHandshakePacket(int protocolVersion, int javaProtocol) implements ServerCustomPacket {
    public static final Key ID = Key.of(BedrockProtocol.HANDSHAKE_CHANNEL);
    private static final ServerboundBedrockHandshakePacket INVALID = new ServerboundBedrockHandshakePacket(-1, -1);
    public static final NetworkCodec<FriendlyByteBuf, ServerboundBedrockHandshakePacket> CODEC = ServerCustomPacket.codec(
            (packet, buf) -> {
                buf.writeInt(packet.protocolVersion);
                buf.writeInt(packet.javaProtocol);
            },
            buf -> buf.readableBytes() == 2 * Integer.BYTES ? new ServerboundBedrockHandshakePacket(buf.readInt(), buf.readInt()) : INVALID
    );

    @Override
    public Key id() {
        return ID;
    }

    @Override
    public NetworkCodec<FriendlyByteBuf, ServerboundBedrockHandshakePacket> codec() {
        return CODEC;
    }

    @Override
    public void handle(NetWorkUser user, ByteBufPacketEvent event) {
        event.setCancelled(true);
        if (this.protocolVersion != BedrockProtocol.VERSION || user.hasBedrockExtension()) {
            return;
        }
        user.setBedrockProtocol(this.protocolVersion);
        if (this.javaProtocol < 0 || !VersionHelper.isOrAbove1_21_4 || !(user instanceof Player player)) {
            return;
        }
        int serverProtocol = VersionHelper.JAVA_PROTOCOL_VERSION;
        boolean sameProtocol = this.javaProtocol == serverProtocol;
        ClientboundItemRewriter rewriter = sameProtocol ? null : CraftEngine.instance().compatibilityManager().getViaVersionClientboundItemRewriter(user, serverProtocol, this.javaProtocol);
        if (!sameProtocol && rewriter == null) {
            return;
        }
        if (Config.bedrockAsyncCreativePacketBuild()) {
            CraftEngine.instance().scheduler().executeAsync(() -> ClientboundBedrockCreativeItemsPacket.send(player, rewriter));
        } else {
            CraftEngine.instance().scheduler().platform().run(() -> ClientboundBedrockCreativeItemsPacket.send(player, rewriter), null, player);
        }
    }
}
