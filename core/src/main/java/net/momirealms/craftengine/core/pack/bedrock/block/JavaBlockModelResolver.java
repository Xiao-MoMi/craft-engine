package net.momirealms.craftengine.core.pack.bedrock.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.*;

public final class JavaBlockModelResolver {
    private final JavaResourcePack resources;
    private final JavaModelResolver models;

    public JavaBlockModelResolver(JavaModelResources resources) {
        this.resources = resources.pack();
        this.models = resources.models();
    }

    public List<Part> resolve(BlockStateWrapper state) throws IOException {
        Key block = state.ownerId();
        JsonObject definition = this.resources.readJson("assets/" + block.namespace() + "/blockstates/" + block.value() + ".json");
        if (definition == null) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.missing", block.asString()));
        }
        Map<String, String> properties = new HashMap<>();
        String identifier = state.getAsString();
        int start = identifier.indexOf('[');
        if (start >= 0) {
            for (String property : identifier.substring(start + 1, identifier.length() - 1).split(",")) {
                String[] pair = property.split("=", 2);
                properties.put(pair[0], pair[1]);
            }
        }
        List<Part> parts = new ArrayList<>();
        if (definition.has("variants")) {
            for (Map.Entry<String, JsonElement> variant : definition.getAsJsonObject("variants").entrySet()) {
                boolean matches = true;
                if (!variant.getKey().isEmpty()) {
                    for (String property : variant.getKey().split(",")) {
                        String[] pair = property.split("=", 2);
                        if (!pair[1].equals(properties.get(pair[0]))) {
                            matches = false;
                            break;
                        }
                    }
                }
                if (matches) {
                    if (!parts.isEmpty()) {
                        throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.multiple", identifier));
                    }
                    parts.add(this.part(variant.getValue()));
                }
            }
            if (parts.isEmpty()) {
                throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.no_match", identifier));
            }
        } else if (definition.has("multipart")) {
            for (JsonElement value : definition.getAsJsonArray("multipart")) {
                JsonObject part = value.getAsJsonObject();
                if (!part.has("when") || matches(part.getAsJsonObject("when"), properties)) {
                    parts.add(this.part(part.get("apply")));
                }
            }
        } else {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.definition", block.asString()));
        }
        return parts;
    }

    private static boolean matches(JsonObject condition, Map<String, String> properties) {
        for (Map.Entry<String, JsonElement> entry : condition.entrySet()) {
            if (entry.getKey().equals("OR") || entry.getKey().equals("AND")) {
                boolean and = entry.getKey().equals("AND");
                boolean result = and;
                for (JsonElement child : entry.getValue().getAsJsonArray()) {
                    boolean matched = matches(child.getAsJsonObject(), properties);
                    if (and != matched) {
                        result = matched;
                        break;
                    }
                }
                if (!result) {
                    return false;
                }
            } else {
                String actual = properties.get(entry.getKey());
                if (actual == null) {
                    throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.property", entry.getKey()));
                }
                String expected = entry.getValue().getAsString();
                boolean negate = expected.startsWith("!");
                boolean matched = false;
                for (String alternative : (negate ? expected.substring(1) : expected).split("\\|")) {
                    if (alternative.equals(actual)) {
                        matched = true;
                        break;
                    }
                }
                if (matched == negate) {
                    return false;
                }
            }
        }
        return true;
    }

    private Part part(JsonElement value) throws IOException {
        if (value.isJsonArray()) {
            if (value.getAsJsonArray().size() != 1) {
                throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.weighted"));
            }
            value = value.getAsJsonArray().get(0);
        }
        JsonObject selected = value.getAsJsonObject();
        int x = selected.has("x") ? selected.get("x").getAsInt() : 0;
        int y = selected.has("y") ? selected.get("y").getAsInt() : 0;
        int z = selected.has("z") ? selected.get("z").getAsInt() : 0;
        if (x % 90 != 0 || y % 90 != 0 || z % 90 != 0) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.rotation"));
        }
        Matrix4f rotation = new Matrix4f().rotationZYX((float) Math.toRadians(-z), (float) Math.toRadians(-y), (float) Math.toRadians(-x));
        JavaModel model = this.models.resolve(Key.of(selected.get("model").getAsString()));
        if (model.kind() != JavaModel.GeometryKind.ELEMENTS) {
            throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.model_renderer", model.id().asString()));
        }
        boolean uvLock = selected.has("uvlock") && selected.get("uvlock").getAsBoolean();
        Matrix4f transform = new Matrix4f().translation(0.5f, 0.5f, 0.5f).mul(rotation).translate(-0.5f, -0.5f, -0.5f);
        return new Part(uvLock ? lockUv(model, rotation) : model, transform);
    }

    private static JavaModel lockUv(JavaModel model, Matrix4fc rotation) {
        List<JavaModel.Element> elements = new ArrayList<>(model.elements().size());
        for (JavaModel.Element element : model.elements()) {
            Map<Direction, JavaModel.Face> faces = new EnumMap<>(Direction.class);
            for (Map.Entry<Direction, JavaModel.Face> entry : element.faces().entrySet()) {
                faces.put(entry.getKey(), lockUv(entry.getValue(), entry.getKey(), rotation));
            }
            elements.add(new JavaModel.Element(element.from(), element.to(), faces, element.rotation()));
        }
        return new JavaModel(model.id(), model.kind(), model.textures(), elements, model.display(), model.guiLight());
    }

    private static JavaModel.Face lockUv(JavaModel.Face face, Direction side, Matrix4fc rotation) {
        // Java's UV lock maps the rotated face back into its own face coordinates, around the texture center.
        Matrix4f faceAction = new Matrix4f(rotation).mul(faceFrame(side));
        Vector3f normal = faceAction.transformDirection(new Vector3f(0, 0, 1));
        Direction target = Math.abs(normal.x) > 0.5f ? (normal.x > 0 ? Direction.EAST : Direction.WEST)
                : (Math.abs(normal.y) > 0.5f ? (normal.y > 0 ? Direction.UP : Direction.DOWN)
                : (normal.z > 0 ? Direction.SOUTH : Direction.NORTH));
        Matrix4f inverse = faceFrame(target).invert().mul(faceAction).invert();
        Vector3f[] corners = new Vector3f[4];
        JavaModel.Uv uv = face.uv();
        for (int i = 0; i < 4; i++) {
            int index = (i + face.rotation() / 90) % 4;
            float u = index < 2 ? uv.u1() : uv.u2();
            float v = index == 0 || index == 3 ? uv.v1() : uv.v2();
            corners[i] = inverse.transformPosition(new Vector3f(u - 8, v - 8, 0)).add(8, 8, 0);
        }
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            Vector3f first = corners[(4 - quadrant) % 4];
            Vector3f second = corners[(5 - quadrant) % 4];
            Vector3f opposite = corners[(6 - quadrant) % 4];
            if (Math.abs(first.x - second.x) < 0.0001f && Math.abs(second.y - opposite.y) < 0.0001f) {
                return new JavaModel.Face(face.texture(), new JavaModel.Uv(first.x, first.y, opposite.x, opposite.y), quadrant * 90, face.tintIndex());
            }
        }
        throw new UnsupportedOperationException(TranslationManager.message("resource_pack.bedrock.block.uv"));
    }

    private static Matrix4f faceFrame(Direction side) {
        return switch (side) {
            case SOUTH -> new Matrix4f();
            case EAST -> new Matrix4f().rotationY((float) (Math.PI / 2));
            case WEST -> new Matrix4f().rotationY((float) (-Math.PI / 2));
            case NORTH -> new Matrix4f().rotationY((float) Math.PI);
            case UP -> new Matrix4f().rotationX((float) (-Math.PI / 2));
            case DOWN -> new Matrix4f().rotationX((float) (Math.PI / 2));
        };
    }

    public record Part(JavaModel model, Matrix4fc transform) {
    }
}
