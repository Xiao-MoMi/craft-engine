package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.PngOptimizer;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class BedrockGeometryWriter {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final String BASE_FORMAT = "1.12.0";
    private static final String BINDING_FORMAT = "1.16.0";
    private static final String UV_ROTATION_FORMAT = "1.21.0";

    private BedrockGeometryWriter() {
    }

    public static void write(BedrockModelConverter.Result result, Path geometryPath, Path texturePath) throws IOException {
        write(result, geometryPath, texturePath, rootBone(result.geometry()), result.geometry().bounds());
    }

    public static void write(BedrockModelConverter.Result result, Path geometryPath, Path texturePath,
                             List<BedrockGeometry.Bone> bones, BedrockGeometry.Bounds bounds) throws IOException {
        Files.createDirectories(geometryPath.toAbsolutePath().getParent());
        GsonHelper.writeJsonFile(toJson(result.geometry(), bones, bounds), geometryPath);
        Path parent = texturePath.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        try (OutputStream output = Files.newOutputStream(texturePath)) {
            new PngOptimizer(result.atlas().image(), 0).write(output);
        }
    }

    public static JsonObject toJson(BedrockGeometry geometry) {
        return toJson(geometry, rootBone(geometry), geometry.bounds());
    }

    private static List<BedrockGeometry.Bone> rootBone(BedrockGeometry geometry) {
        return List.of(new BedrockGeometry.Bone("root", null, VectorUtils.ZERO, null, geometry.cubes()));
    }

    public static JsonObject toJson(BedrockGeometry geometry, List<BedrockGeometry.Bone> rig, BedrockGeometry.Bounds bounds) {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", geometry.identifier());
        description.addProperty("texture_width", geometry.textureWidth());
        description.addProperty("texture_height", geometry.textureHeight());
        description.addProperty("visible_bounds_width", bounds.width());
        description.addProperty("visible_bounds_height", bounds.height());
        description.add("visible_bounds_offset", vector(bounds.offset()));
        JsonArray bones = new JsonArray();
        boolean rotatedUv = false;
        boolean bound = false;
        for (BedrockGeometry.Bone bone : rig) {
            JsonObject boneJson = new JsonObject();
            boneJson.addProperty("name", bone.name());
            boneJson.add("pivot", vector(bone.pivot()));
            if (bone.parent() != null) {
                boneJson.addProperty("parent", bone.parent());
            }
            if (bone.binding() != null) {
                boneJson.addProperty("binding", bone.binding());
                bound = true;
            }
            if (!bone.cubes().isEmpty()) {
                JsonArray cubes = new JsonArray();
                for (BedrockGeometry.Cube cube : bone.cubes()) {
                    cubes.add(writeCube(cube));
                    for (BedrockGeometry.Face face : cube.faces().values()) {
                        rotatedUv |= face.rotation() != 0;
                    }
                }
                boneJson.add("cubes", cubes);
            }
            bones.add(boneJson);
        }
        JsonObject model = new JsonObject();
        model.add("description", description);
        model.add("bones", bones);
        JsonArray models = new JsonArray();
        models.add(model);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", rotatedUv ? UV_ROTATION_FORMAT : (bound ? BINDING_FORMAT : BASE_FORMAT));
        root.add("minecraft:geometry", models);
        return root;
    }

    private static JsonObject writeCube(BedrockGeometry.Cube cube) {
        JsonObject result = new JsonObject();
        result.add("origin", vector(cube.origin()));
        result.add("size", vector(cube.size()));
        if (cube.rotation() != null) {
            result.add("pivot", vector(cube.rotation().pivot()));
            result.add("rotation", vector(cube.rotation().angles()));
        }
        JsonObject faces = new JsonObject();
        for (Direction direction : DIRECTIONS) {
            BedrockGeometry.Face face = cube.faces().get(direction);
            if (face == null) {
                continue;
            }
            JsonObject uv = new JsonObject();
            uv.add("uv", pair(face.u(), face.v()));
            uv.add("uv_size", pair(face.width(), face.height()));
            if (face.rotation() != 0) {
                uv.addProperty("uv_rotation", face.rotation());
            }
            if (face.material() != null) {
                uv.addProperty("material_instance", face.material());
            }
            faces.add(direction.name().toLowerCase(Locale.ROOT), uv);
        }
        result.add("uv", faces);
        return result;
    }

    private static JsonArray vector(Vector3f vector) {
        JsonArray result = pair(vector.x(), vector.y());
        addNumber(result, vector.z());
        return result;
    }

    private static JsonArray pair(float first, float second) {
        JsonArray result = new JsonArray();
        addNumber(result, first);
        addNumber(result, second);
        return result;
    }

    private static void addNumber(JsonArray array, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite geometry coordinate: " + value);
        }
        array.add(value == 0 /* normalize */ ? 0 : value);
    }
}
