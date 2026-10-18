package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Vector3f;

public final class BedrockBlockItemModel {
    private static final DisplayPosition[] VIEWS = {
            DisplayPosition.GUI,
            DisplayPosition.GROUND,
            DisplayPosition.FIXED,
            DisplayPosition.FIRSTPERSON_RIGHTHAND,
            DisplayPosition.FIRSTPERSON_LEFTHAND,
            DisplayPosition.THIRDPERSON_RIGHTHAND,
            DisplayPosition.THIRDPERSON_LEFTHAND
    };
    private static final String[] VIEW_NAMES = {
            "gui",
            "ground",
            "fixed",
            "firstperson_righthand",
            "firstperson_lefthand",
            "thirdperson_righthand",
            "thirdperson_lefthand"
    };

    private BedrockBlockItemModel() {
    }

    public static JsonObject convert(BedrockModelConverter.Result converted, String identifier, int textureWidth) {
        return convert(converted, identifier, textureWidth, converted.geometry().textureHeight());
    }

    public static JsonObject convert(BedrockModelConverter.Result converted, String identifier, int textureWidth, int textureHeight) {
        return convert(converted, identifier, textureWidth, textureHeight, true);
    }

    public static JsonObject convert(BedrockModelConverter.Result converted, String identifier, int textureWidth, int textureHeight, boolean held) {
        BedrockGeometry geometry = converted.geometry();
        if (geometry.cubes().isEmpty()) {
            throw new IllegalArgumentException("Empty block item geometry: " + converted.source().id());
        }
        // Fit rotated cubes inside the base block around the Java model center, then restore their size in display transforms.
        float scale = BedrockModelTransform.blockItemScale(geometry.cubes());
        JsonObject transforms = new JsonObject();
        for (int index = 0; index < (held ? VIEWS.length : 3); index++) {
            JavaModel.Transform transform = converted.source().display().getOrDefault(VIEWS[index], JavaModel.Transform.identity());
            Vector3f displayScale = new Vector3f(transform.scale()).div(scale);
            if (Math.min(displayScale.x(), Math.min(displayScale.y(), displayScale.z())) < 0
                    || Math.max(displayScale.x(), Math.max(displayScale.y(), displayScale.z())) > 4) {
                throw new IllegalArgumentException("Block item " + VIEW_NAMES[index] + " scale exceeds [0,4] after geometry normalization: " + converted.source().id());
            }
            JsonObject display = new JsonObject();
            // Blockbench DisplaySlot.exportBedrock retains display angles; animation bone angle signs do not apply here.
            display.add("rotation", VectorUtils.toJson(transform.rotation()));
            display.add("translation", VectorUtils.toJson(transform.translation()));
            display.add("scale", VectorUtils.toJson(displayScale));
            display.add("rotation_pivot", VectorUtils.toJson(VectorUtils.ZERO));
            display.add("scale_pivot", VectorUtils.toJson(VectorUtils.ZERO));
            if (VIEWS[index] == DisplayPosition.GUI) {
                display.addProperty("fit_to_frame", false);
            }
            transforms.add(VIEW_NAMES[index], display);
        }
        JsonObject root = BedrockGeometryWriter.toJson(BedrockBlockGeometry.convert(geometry));
        root.addProperty("format_version", "1.21.0");
        JsonObject model = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject description = model.getAsJsonObject("description");
        description.addProperty("identifier", identifier);
        description.addProperty("texture_width", textureWidth);
        description.addProperty("texture_height", textureHeight);
        model.add("item_display_transforms", transforms);
        if (scale != 1) {
            for (JsonElement bone : model.getAsJsonArray("bones")) {
                for (JsonElement element : bone.getAsJsonObject().getAsJsonArray("cubes")) {
                    JsonObject cube = element.getAsJsonObject();
                    scale(cube.getAsJsonArray("origin"), scale, 8);
                    scale(cube.getAsJsonArray("size"), scale, 0);
                    if (cube.has("pivot")) {
                        scale(cube.getAsJsonArray("pivot"), scale, 8);
                    }
                }
            }
        }
        return root;
    }

    private static void scale(JsonArray vector, float scale, float centerY) {
        vector.set(0, new JsonPrimitive(vector.get(0).getAsFloat() * scale));
        vector.set(1, new JsonPrimitive((vector.get(1).getAsFloat() - centerY) * scale + centerY));
        vector.set(2, new JsonPrimitive(vector.get(2).getAsFloat() * scale));
    }
}
