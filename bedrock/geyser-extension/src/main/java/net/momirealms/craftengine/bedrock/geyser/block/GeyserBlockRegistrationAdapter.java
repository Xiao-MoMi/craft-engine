package net.momirealms.craftengine.bedrock.geyser.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.component.PlacementConditions;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserBlockRegistrationAdapter {

    private GeyserBlockRegistrationAdapter() {
    }

    public static String registerBlock(JsonObject display, GeyserDefineCustomBlocksEvent event) {
        boolean world = display.has("java_state");
        CustomBlockComponents.Builder components = CustomBlockComponents.builder()
                .geometry(GeometryComponent.builder().identifier(display.get(GEOMETRY).getAsString()).build());
        if (world) {
            // CE owns mining completion; the client must not destroy the visual carrier independently.
            components.selectionBox(BoxComponent.fullBox()).collisionBoxes(BoxComponent.fullBox())
                    .destructibleByMining(Float.MAX_VALUE).placeAir(false)
                    .lightEmission(display.get("light_emission").getAsInt());
            if (display.has("collision_boxes")) {
                List<BoxComponent> boxes = new ArrayList<>();
                for (JsonElement box : display.getAsJsonArray("collision_boxes")) {
                    boxes.add(box(box));
                }
                if (boxes.isEmpty()) {
                    boxes.add(BoxComponent.emptyBox());
                }
                components.collisionBoxes(boxes).selectionBox(box(display.get("selection_box")))
                        .lightDampening(display.get("light_dampening").getAsInt())
                        .friction(display.get("friction").getAsFloat());
            }
        } else {
            components.selectionBox(null).collisionBoxes()
                    .placementFilter(List.of(new PlacementConditions(Set.of(), new LinkedHashMap<>())))
                    .placeAir(true);
        }
        JsonObject materials = display.getAsJsonObject(MATERIALS);
        for (Map.Entry<String, JsonElement> entry : materials.entrySet()) {
            MaterialInstance material = MaterialInstance.builder()
                    .texture(entry.getValue().getAsString())
                    .renderMethod("alpha_test_single_sided")
                    .faceDimming(world)
                    .ambientOcclusionExponent(world ? 1 : 0)
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
        if (world) {
            event.registerOverride(display.get("java_state").getAsString(), block.defaultBlockState());
        }
        return block.identifier();
    }

    private static BoxComponent box(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return BoxComponent.emptyBox();
        }
        JsonObject box = value.getAsJsonObject();
        JsonArray origin = box.getAsJsonArray("origin");
        JsonArray size = box.getAsJsonArray("size");
        return new BoxComponent(origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat(),
                size.get(0).getAsFloat(), size.get(1).getAsFloat(), size.get(2).getAsFloat());
    }

}
