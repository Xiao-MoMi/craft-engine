package net.momirealms.craftengine.bedrock.geyser.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;

import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserBlockRegistrationAdapter {

    private GeyserBlockRegistrationAdapter() {
    }

    public static String registerBlock(JsonObject display, GeyserDefineCustomBlocksEvent event) {
        CustomBlockComponents.Builder components = CustomBlockComponents.builder()
                .geometry(GeometryComponent.builder().identifier(display.get(GEOMETRY).getAsString()).build())
                .selectionBox(null)
                .collisionBoxes()
                .placeAir(true);
        JsonObject materials = display.getAsJsonObject(MATERIALS);
        for (Map.Entry<String, JsonElement> entry : materials.entrySet()) {
            MaterialInstance material = MaterialInstance.builder()
                    .texture(entry.getValue().getAsString())
                    .renderMethod("alpha_test_single_sided")
                    .faceDimming(false)
                    .ambientOcclusionExponent(0)
                    .build();
            components.materialInstance(entry.getKey(), material);
            if (entry.getKey().equals("layer0")) {
                components.materialInstance("*", material);
            }
        }
        CustomBlockData block = CustomBlockData.builder()
                .name(display.get(NAME).getAsString())
                .includedInCreativeInventory(false)
                .components(components.build())
                .build();
        event.register(block);
        return block.identifier();
    }

}
