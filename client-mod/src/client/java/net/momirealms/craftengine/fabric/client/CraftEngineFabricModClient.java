package net.momirealms.craftengine.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.FoliageColors;
import net.momirealms.craftengine.fabric.client.blocks.CustomBlock;
import net.momirealms.craftengine.fabric.client.config.ModConfig;
import net.momirealms.craftengine.fabric.client.network.CraftEnginePayload;

import java.nio.charset.StandardCharsets;

public class CraftEngineFabricModClient implements ClientModInitializer {
    public static final String MOD_ID = "craftengine";

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.configurationS2C().register(CraftEnginePayload.ID, CraftEnginePayload.CODEC);
        PayloadTypeRegistry.configurationC2S().register(CraftEnginePayload.ID, CraftEnginePayload.CODEC);
        registerRenderLayer();
        ClientConfigurationConnectionEvents.START.register(CraftEngineFabricModClient::initChannel);
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
        if (ModConfig.enableNetwork) {
            registerChannel(handler);
        } else {
            ClientConfigurationNetworking.unregisterGlobalReceiver(CraftEnginePayload.ID);
        }
    }

    private static void registerChannel(ClientConfigurationNetworkHandler handler) {
        ClientConfigurationNetworking.send(new CraftEnginePayload((":" + Block.STATE_IDS.size() + ":init").getBytes(StandardCharsets.UTF_8)));
    }
}
