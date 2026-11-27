package net.momirealms.craftengine.bedrock.geyser.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.component.*;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;

import java.util.*;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserBlockRegistrationAdapter {

    private GeyserBlockRegistrationAdapter() {
    }

    public static String registerBlock(JsonObject display, GeyserDefineCustomBlocksEvent event, boolean vanillaOverride) {
        boolean hasJavaBlockState = display.has("java_state");
        CustomBlockComponents.Builder components = CustomBlockComponents.builder()
                .geometry(GeometryComponent.builder().identifier(display.get(GEOMETRY).getAsString()).build());
        if (hasJavaBlockState) {
            // CraftEngine 自定义方块由扩展接管挖掘，原版覆盖沿用方块原本的硬度
            float hardness = vanillaOverride ? display.get("hardness").getAsFloat() : -1;
            components.selectionBox(BoxComponent.fullBox())
                    .collisionBoxes(BoxComponent.fullBox())
                    .destructibleByMining(hardness < 0 ? Float.MAX_VALUE : hardness)
                    .placeAir(false)
                    .lightEmission(display.get("light_emission").getAsInt());
            if (display.has("collision_boxes")) {
                List<BoxComponent> boxes = new ArrayList<>(4);
                for (JsonElement box : display.getAsJsonArray("collision_boxes")) {
                    boxes.add(box(box));
                }
                if (boxes.isEmpty()) {
                    boxes.add(BoxComponent.emptyBox());
                }
                components.collisionBoxes(boxes)
                        .selectionBox(box(display.get("selection_box")))
                        .lightDampening(display.get("light_dampening").getAsInt())
                        .friction(display.get("friction").getAsFloat());
            }
        } else {
            // 这个定义只用来渲染物品模型
            components.selectionBox(null)
                    .collisionBoxes()
                    .placementFilter(List.of(new PlacementConditions(Set.of(), new LinkedHashMap<>())))
                    .placeAir(true);
        }
        JsonObject materials = display.getAsJsonObject(MATERIALS);
        JsonObject tintMethods = display.getAsJsonObject(TINT_METHODS);
        for (Map.Entry<String, JsonElement> entry : materials.entrySet()) {
            MaterialInstance.Builder builder = MaterialInstance.builder()
                    .texture(entry.getValue().getAsString())
                    .renderMethod("alpha_test_single_sided")
                    .faceDimming(hasJavaBlockState)
                    .ambientOcclusionExponent(hasJavaBlockState ? 1 : 0);
            if (tintMethods != null && tintMethods.has(entry.getKey())) {
                builder.tintMethod(tintMethods.get(entry.getKey()).getAsString());
            }
            MaterialInstance material = builder.build();
            components.materialInstance(entry.getKey(), material);
            // 没有指定材质槽的面就会用 layer0 的贴图
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
        if (hasJavaBlockState) {
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
        return new BoxComponent(
                origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat(),
                size.get(0).getAsFloat(), size.get(1).getAsFloat(), size.get(2).getAsFloat()
        );
    }
}
