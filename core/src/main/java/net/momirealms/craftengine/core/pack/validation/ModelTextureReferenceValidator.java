package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.atlas.SimplifiedModelFile;
import net.momirealms.craftengine.core.pack.atlas.TexturedModel;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ModelTextureReferenceValidator {

    private ModelTextureReferenceValidator() {
    }

    public static boolean fixMissingParticleTexture(JsonObject json, TexturedModel model) {
        if (model.textures.containsKey(ModelJsonKeys.PARTICLE)) {
            return false;
        }
        String replacement = findParticleTexture(model);
        if (replacement == null) {
            return false;
        }
        JsonObject textures = json.getAsJsonObject(ModelJsonKeys.TEXTURES);
        if (textures == null) {
            textures = new JsonObject();
            json.add(ModelJsonKeys.TEXTURES, textures);
        }
        textures.addProperty(ModelJsonKeys.PARTICLE, "#" + replacement);
        model.setTextureReference(ModelJsonKeys.PARTICLE, replacement);
        return true;
    }

    private static @Nullable String findParticleTexture(TexturedModel model) {
        if (isUsable(model.textures.get("layer0"))) {
            return "layer0";
        }
        for (SimplifiedModelFile.FaceTexture face : model.faces()) {
            if (isUsable(model.textures.get(face.slot()))) {
                return face.slot();
            }
        }
        String first = null;
        for (Map.Entry<String, Key> entry : model.textures.entrySet()) {
            if (isUsable(entry.getValue()) && (first == null || entry.getKey().compareTo(first) < 0)) {
                first = entry.getKey();
            }
        }
        return first;
    }

    private static boolean isUsable(@Nullable Key texture) {
        return texture != null && !texture.equals(TexturedModel.EMPTY.path);
    }

    public static List<Problem> validate(TexturedModel model) {
        List<Problem> problems = new ArrayList<>();
        Map<String, ReferenceFailure> failures = new HashMap<>();
        model.forEachReference((slot, target) -> {
            if (!model.textures.containsKey(slot)) {
                check(model, slot, ModelJsonKeys.TEXTURES + "." + slot, problems, failures);
            }
        });
        for (SimplifiedModelFile.FaceTexture face : model.faces()) {
            if (!model.textures.containsKey(face.slot())) {
                check(model, face.slot(), face.location(), problems, failures);
            }
        }
        if (!model.textures.containsKey(ModelJsonKeys.PARTICLE) && model.reference(ModelJsonKeys.PARTICLE) == null) {
            check(model, ModelJsonKeys.PARTICLE, ModelJsonKeys.PARTICLE, problems, failures);
        }
        return problems;
    }

    private static void check(TexturedModel model, String slot, String location, List<Problem> problems,
                              Map<String, ReferenceFailure> failures) {
        ReferenceFailure failure = failures.get(slot);
        if (failure == null) {
            failure = traceFailure(model, slot);
            failures.put(slot, failure);
        }
        problems.add(new Problem(location, failure.chain, failure.cycle));
    }

    private static ReferenceFailure traceFailure(TexturedModel model, String slot) {
        Set<String> visited = new LinkedHashSet<>();
        String current = slot;
        while (visited.add(current)) {
            String target = model.reference(current);
            if (target == null) {
                return new ReferenceFailure("#" + String.join(" -> #", visited), false);
            }
            current = target;
        }
        return new ReferenceFailure("#" + String.join(" -> #", visited) + " -> #" + current, true);
    }

    private record ReferenceFailure(String chain, boolean cycle) {
    }

    public record Problem(String location, String chain, boolean cycle) {
    }
}
