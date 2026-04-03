package net.momirealms.craftengine.bukkit.plugin.network.mod.protocol;

import net.momirealms.craftengine.core.plugin.network.mod.ModPacket;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ModPackets;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public record CancelBlockUpdatePacket(boolean enabled) implements ModPacket {
    public static final ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> TYPE = ResourceKey.create(
            BuiltInRegistries.MOD_PACKET.key().location(), Key.of("craftengine", "cancel_block_update")
    );
    public static final NetworkCodec<FriendlyByteBuf, CancelBlockUpdatePacket> CODEC = ModPacket.codec(
            CancelBlockUpdatePacket::encode,
            CancelBlockUpdatePacket::new
    );

    private CancelBlockUpdatePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.enabled);
    }

    @Override
    public ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> type() {
        return TYPE;
    }

    @Override
    public void handle(NetWorkUser user) {
        if (!this.enabled) return;
        ModPackets.sendPacket(user, this);
    }
}
