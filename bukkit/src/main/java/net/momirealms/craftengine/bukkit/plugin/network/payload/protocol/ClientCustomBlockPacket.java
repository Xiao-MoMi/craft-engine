package net.momirealms.craftengine.bukkit.plugin.network.payload.protocol;


import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslationArgument;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.network.payload.PayloadHelper;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.network.ModPacket;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.codec.NetworkCodec;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockAndTintGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.ChunkSourceProxy;
import net.momirealms.craftengine.proxy.paper.chunk.system.entity.RegionizedPlayerChunkLoaderProxy;
import org.bukkit.entity.Player;

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
        int vanillaBlockRegistrySize = BlockStateUtils.vanillaBlockStateCount();
        if (this.vanillaSize != vanillaBlockRegistrySize) {
            user.kick(Component.translatable(
                    "disconnect.craftengine.vanilla_block_registry_mismatch",
                    TranslationArgument.numeric(this.vanillaSize),
                    TranslationArgument.numeric(vanillaBlockRegistrySize)
            ));
            return;
        }
        int serverBlockRegistrySize = RegistryUtils.currentBlockRegistrySize();
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
        PayloadHelper.sendData(user, BukkitBlockManager.instance().cachedVisualBlockStatePacket());
        if (!VersionHelper.isOrAbove1_20_2()) {
            // 因为旧版本没有配置阶段需要重新发送区块
            CraftEngine.instance().scheduler().executeSync(() -> {
                try {
                    Object chunkLoader = ServerPlayerProxy.INSTANCE.getChunkLoader(user.serverPlayer());
                    LongOpenHashSet sentChunks = RegionizedPlayerChunkLoaderProxy.PlayerChunkLoaderDataProxy.INSTANCE.getSentChunks(chunkLoader);
                    if (sentChunks.isEmpty()) {
                        return;
                    }
                    sentChunks = sentChunks.clone();
                    Object serverLevel = CraftWorldProxy.INSTANCE.getWorld(((Player) user.platformPlayer()).getWorld());
                    Object lightEngine = BlockAndTintGetterProxy.INSTANCE.getLightEngine(serverLevel);
                    Object chunkSource = ServerLevelProxy.INSTANCE.getChunkSource(serverLevel);
                    for (long chunkPos : sentChunks) {
                        int chunkX = (int) chunkPos;
                        int chunkZ = (int) (chunkPos >> 32);
                        Object levelChunk = ChunkSourceProxy.INSTANCE.getChunk(chunkSource, chunkX, chunkZ, false);
                        Object packet = NetworkReflections.constructor$ClientboundLevelChunkWithLightPacket.newInstance(levelChunk, lightEngine, null, null);
                        user.sendPacket(packet, true);
                    }
                } catch (Exception e) {
                    CraftEngine.instance().logger().warn("Failed to refresh chunk for player " + user.name(), e);
                }
            });
        }
    }
}
