package net.momirealms.craftengine.core.pack.model.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ModelUtils;
import net.momirealms.craftengine.core.util.ResourcePackJsonUtils;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static net.momirealms.craftengine.core.pack.model.ModelJsonKeys.ELEMENTS;
import static net.momirealms.craftengine.core.pack.model.ModelJsonKeys.PARENT;

public record OffsetBlockModel(Key model, float x, float y, float z) {

    public Key id() {
        String offset = (this.x + "_" + this.y + "_" + this.z).toLowerCase(Locale.ROOT);
        return Key.withCraftEngineNamespace("block/offset/" + this.model.namespace() + "/" + this.model.value() + "/" + offset);
    }

    public static void generateAll(Path pack, Set<OffsetBlockModel> requests) throws IOException {
        if (requests.isEmpty()) {
            return;
        }
        List<Path> roots = FileUtils.collectOverlays(Files.createDirectories(pack));
        roots.addFirst(pack);
        Set<OffsetBlockModel> generated = new HashSet<>();
        for (OffsetBlockModel request : requests) {
            request.generate(roots, generated, request.model);
        }
    }

    private void generate(List<Path> roots, Set<OffsetBlockModel> generated, Key requestedModel) throws IOException {
        if (!generated.add(this)) {
            return;
        }
        String sourcePath = ResourcePackUtils.modelPath(this.model);
        String generatedPath = ResourcePackUtils.modelPath(this.id());
        for (int index = 0; index < roots.size(); index++) {
            Path root = roots.get(index);
            Path source = root.resolve(sourcePath);
            JsonObject modelJson;
            if (Files.isRegularFile(source)) {
                modelJson = ResourcePackJsonUtils.readJsonObjectOrWarn(source);
            } else if (index == 0) {
                JsonObject preset = this.vanillaModel();
                modelJson = preset == null ? null : preset.deepCopy();
            } else {
                continue;
            }

            Path target = root.resolve(generatedPath);
            if (Files.exists(target)) {
                TranslationManager.warn("resource_pack.model_generation.conflict", target.toString());
                continue;
            }
            if (modelJson == null) {
                modelJson = this.inheritSource();
            } else if (modelJson.get(ELEMENTS) instanceof JsonArray elements && !elements.isEmpty()) {
                modelJson = this.shiftGeometry(modelJson, elements, requestedModel);
            } else if (modelJson.get(PARENT) instanceof JsonPrimitive parentId && parentId.isString()) {
                // Mirror the parent reference in each overlay so the client selects the same geometry and texture overrides.
                Key parent = Key.of(parentId.getAsString());
                OffsetBlockModel shiftedParent = new OffsetBlockModel(parent, this.x, this.y, this.z);
                shiftedParent.generate(roots, generated, requestedModel);
                modelJson.addProperty(PARENT, shiftedParent.id().asString());
            }
            ResourcePackUtils.writeJson(modelJson, target);
        }
    }

    private JsonObject shiftGeometry(JsonObject modelJson, JsonArray elements, Key requestedModel) {
        double[] applied;
        try {
            applied = ModelUtils.offsetElements(elements, this.x, this.y, this.z);
        } catch (RuntimeException e) {
            TranslationManager.warn("resource_pack.model_offset.invalid_elements", this.model.asString());
            return this.inheritSource();
        }
        if (applied[0] != this.x || applied[1] != this.y || applied[2] != this.z) {
            TranslationManager.warn("resource_pack.model_offset.clamped", requestedModel.asString(),
                    Arrays.toString(new float[]{this.x, this.y, this.z}),
                    Arrays.toString(new float[]{(float) applied[0], (float) applied[1], (float) applied[2]}));
        }
        return modelJson;
    }

    private JsonObject inheritSource() {
        JsonObject json = new JsonObject();
        json.addProperty(PARENT, this.model.asMinimalString());
        return json;
    }

    @Nullable
    private JsonObject vanillaModel() {
        String path = this.model.value();
        if (path.startsWith("block/")) {
            return AbstractPackManager.PRESET_MODELS_BLOCK.get(this.model.withSameNamespace(path.substring(6)));
        }
        if (path.startsWith("item/")) {
            return AbstractPackManager.PRESET_MODERN_MODELS_ITEM.get(this.model.withSameNamespace(path.substring(5)));
        }
        return null;
    }
}
