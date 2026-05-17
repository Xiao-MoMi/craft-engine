package net.momirealms.craftengine.core.plugin.network.mod.protocol;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslationArgument;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.plugin.network.mod.ModPacket;
import net.momirealms.craftengine.core.plugin.network.mod.ModPackets;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.*;

public record ClientCustomBlockPacket(int vanillaSize, int currentSize) implements ModPacket {
    public static final ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> TYPE = ResourceKey.create(
            BuiltInRegistries.MOD_PACKET.key().location(), Key.of("craftengine", "client_custom_block")
    );
    public static final NetworkCodec<FriendlyByteBuf, ClientCustomBlockPacket> CODEC = ModPacket.codec(
            ClientCustomBlockPacket::encode,
            ClientCustomBlockPacket::new
    );

    private ClientCustomBlockPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.vanillaSize);
        buf.writeInt(this.currentSize);
    }

    @Override
    public ResourceKey<NetworkCodec<FriendlyByteBuf, ? extends ModPacket>> type() {
        return TYPE;
    }

    @Override
    public void handle(NetWorkUser user) {
        if (user.clientModEnabled()) return; // 防止滥用
        int vanillaBlockRegistrySize = CraftEngine.instance().blockManager().vanillaBlockStateCount();
        if (this.vanillaSize != vanillaBlockRegistrySize) {
            user.kick(Component.translatable(
                    "disconnect.craftengine.vanilla_block_registry_mismatch",
                    TranslationArgument.numeric(this.vanillaSize),
                    TranslationArgument.numeric(vanillaBlockRegistrySize)
            ));
            return;
        }
        int serverBlockRegistrySize = CraftEngine.instance().blockManager().currentBlockRegistrySize();
        if (this.currentSize < serverBlockRegistrySize) {
            user.kick(Component.translatable(
                    "disconnect.craftengine.current_block_registry_mismatch",
                    TranslationArgument.numeric(this.currentSize),
                    TranslationArgument.numeric(serverBlockRegistrySize)
            ));
            return;
        }
        user.setClientModState(true);
        user.setClientBlockList(new IntIdentityList(this.currentSize));
        ModPackets.sendPacket(user, CraftEngine.instance().blockManager().cachedVisualBlockStatePacket());
        if (!VersionHelper.isOrAbove1_20_2) {
            // 因为旧版本没有配置阶段需要重新发送区块
            CraftEngine.instance().scheduler().platform().run(user::resendChunks, null, (Player) user);
        }
    }
}
