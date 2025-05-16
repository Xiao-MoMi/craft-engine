package net.momirealms.craftengine.fabric.client;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.FoliageColors;
import net.momirealms.craftengine.fabric.client.blocks.CustomBlock;
import net.momirealms.craftengine.fabric.client.config.ModConfig;
import net.momirealms.craftengine.fabric.client.network.CraftEnginePayload;
import net.momirealms.craftengine.fabric.client.util.NetWorkDataTypes;

@Environment(EnvType.CLIENT)
public class CraftEngineFabricModClient implements ClientModInitializer {
    public static final String MOD_ID = "craftengine";
    public static boolean serverInstalled = false;

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.configurationS2C().register(CraftEnginePayload.ID, CraftEnginePayload.CODEC);
        PayloadTypeRegistry.configurationC2S().register(CraftEnginePayload.ID, CraftEnginePayload.CODEC);
        registerRenderLayer();
        ClientConfigurationConnectionEvents.START.register(CraftEngineFabricModClient::initChannel);
        ClientConfigurationNetworking.registerGlobalReceiver(CraftEnginePayload.ID, CraftEngineFabricModClient::handleReceiver);
        ClientPlayConnectionEvents.DISCONNECT.register((client, handler) -> serverInstalled = false);
    }

    public static void registerRenderLayer() {
        Registries.BLOCK.forEach(block -> {
            Identifier id = Registries.BLOCK.getId(block);
            if (block instanceof CustomBlock customBlock) {
                if (customBlock.isTransparent()) {
                    BlockRenderLayerMap.INSTANCE.putBlock(customBlock, RenderLayer.getCutoutMipped());
                }
                if (id.getPath().contains("leaves")) {
                    registerColor(block);
                }
            }
        });
    }

    public static void registerColor(Block block) {
        ColorProviderRegistry.BLOCK.register(
                (state, world, pos, tintIndex) -> {
                    if (world != null && pos != null) {
                        return BiomeColors.getFoliageColor(world, pos);
                    }
                    return FoliageColors.DEFAULT;
                },
                block
        );
    }

    private static void initChannel(ClientConfigurationNetworkHandler handler, MinecraftClient client) {
        if (!ModConfig.enableNetwork && !ModConfig.enableCancelBlockUpdate) {
            ClientConfigurationNetworking.unregisterGlobalReceiver(CraftEnginePayload.ID);
            return;
        }

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        if (ModConfig.enableNetwork) {
            NetWorkDataTypes<Integer> type = NetWorkDataTypes.CLIENT_CUSTOM_BLOCK;
            type.writeType(buf);
            type.encode(buf, Block.STATE_IDS.size());
        } else if (ModConfig.enableCancelBlockUpdate) {
            NetWorkDataTypes<Boolean> type = NetWorkDataTypes.CANCEL_BLOCK_UPDATE;
            type.writeType(buf);
            type.encode(buf, true);
        }

        ClientConfigurationNetworking.send(new CraftEnginePayload(buf.array()));
    }

    private static void handleReceiver(CraftEnginePayload payload, ClientConfigurationNetworking.Context context) {
        byte[] data = payload.data();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(data));
        NetWorkDataTypes<?> type = NetWorkDataTypes.readType(buf);
        if (type == NetWorkDataTypes.CANCEL_BLOCK_UPDATE) {
            serverInstalled = type.as(Boolean.class).decode(buf);
        }
    }
}
