package net.momirealms.craftengine.bedrock.geyser;

import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.geyser.api.util.Identifier;

import java.lang.reflect.Method;
import java.util.Map;

final class ExperimentalBlockItemAdapter {
    private Method geyserComponent;

    String registerBlock(BedrockPackMappings.DisplayBlock display, GeyserDefineCustomBlocksEvent event) {
        CustomBlockComponents.Builder components = CustomBlockComponents.builder()
                .geometry(GeometryComponent.builder().identifier(display.geometry()).build())
                .materialInstance("*", material(display.texture()))
                .selectionBox(null)
                .collisionBoxes()
                .placeAir(true);
        for (Map.Entry<String, String> material : display.materials().entrySet()) {
            components.materialInstance(material.getKey(), material(material.getValue()));
        }
        CustomBlockData block = CustomBlockData.builder()
                .name(display.name())
                .includedInCreativeInventory(false)
                .components(components.build())
                .build();
        event.register(block);
        return block.identifier();
    }

    private static MaterialInstance material(String texture) {
        return MaterialInstance.builder()
                .texture(texture)
                .renderMethod("alpha_test_single_sided")
                .faceDimming(false)
                .ambientOcclusionExponent(0)
                .build();
    }

    void apply(CustomItemDefinition.Builder builder, String blockIdentifier) throws ReflectiveOperationException {
        // Geyser 2.11.3 uses this internal entry point for block-backed custom skull items.
        // Keep the bypass here: the public component method intentionally rejects BLOCK_PLACER on vanilla items.
        if (this.geyserComponent == null) {
            this.geyserComponent = builder.getClass().getMethod("geyserComponent", ItemDataComponent.class, Object.class);
        }
        this.geyserComponent.invoke(builder, GeyserItemDataComponents.BLOCK_PLACER, GeyserBlockPlacer.of(Identifier.of(blockIdentifier), true));
    }
}
