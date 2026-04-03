package net.momirealms.craftengine.bukkit.plugin.network.mod.protocol;


import net.momirealms.craftengine.core.plugin.network.mod.ModPacket;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IntIdentityList;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public record ClientBlockStateSizePacket(int blockStateSize) implements ModPacket {
    public static final ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> TYPE = ResourceKey.create(
            BuiltInRegistries.MOD_PACKET.key().location(), Key.of("craftengine", "client_block_state_size")
    );
    public static final NetworkCodec<FriendlyByteBuf, ClientBlockStateSizePacket> CODEC = ModPacket.codec(
            ClientBlockStateSizePacket::encode,
            ClientBlockStateSizePacket::new
    );

    private ClientBlockStateSizePacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.blockStateSize);
    }

    @Override
    public ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> type() {
        return TYPE;
    }

    @Override
    public void handle(NetWorkUser user) {
        user.setClientBlockList(new IntIdentityList(this.blockStateSize));
    }
}
