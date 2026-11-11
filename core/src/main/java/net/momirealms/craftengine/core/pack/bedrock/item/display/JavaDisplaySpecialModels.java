package net.momirealms.craftengine.core.pack.bedrock.item.display;

import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class JavaDisplaySpecialModels {

    private JavaDisplaySpecialModels() {
    }

    static JavaModel create(JavaModelResources resources, JavaModel base, String type) throws IOException {
        List<JavaModel.Element> elements = new ArrayList<>();
        JavaModel.Rotation rotation = resources.pack().version().isBelow(MinecraftVersion.V26_1) ? new JavaModel.Rotation(new Vector3f(), new Vector3f(180, 0, 0), false) : null;
        switch (type.replace("minecraft:", "")) {
            case "shield" -> {
                JavaModel.Texture texture = texture(resources, "entity/shield/shield_base_nopattern", "entity/shield_base_nopattern");
                elements.add(cube(texture, 64, 0, 0, -6, -11, -2, 12, 22, 1, rotation));
                elements.add(cube(texture, 64, 26, 0, -1, -3, -1, 2, 6, 6, rotation));
            }
            case "trident", "trident_in_hand" -> {
                JavaModel.Texture texture = texture(resources, "entity/trident/trident", "entity/trident");
                elements.add(cube(texture, 32, 0, 6, -0.5f, 2, -0.5f, 1, 25, 1, rotation));
                elements.add(cube(texture, 32, 4, 0, -1.5f, 0, -0.5f, 3, 2, 1, rotation));
                elements.add(cube(texture, 32, 4, 3, -2.5f, -3, -0.5f, 1, 4, 1, rotation));
                elements.add(cube(texture, 32, 0, 0, -0.5f, -4, -0.5f, 1, 4, 1, rotation));
                elements.add(cube(texture, 32, 4, 3, 1.5f, -3, -0.5f, 1, 4, 1, rotation));
            }
            default -> throw new UnsupportedOperationException("Unsupported special display model " + type);
        }
        return new JavaModel(base.id(), JavaModel.GeometryKind.ELEMENTS, Map.of(), elements, base.display(), base.guiLight());
    }

    private static JavaModel.Texture texture(JavaModelResources resources, String path, String legacyPath) throws IOException {
        try (InputStream input = resources.pack().openResource("assets/minecraft/textures/" + path + ".png")) {
            return new JavaModel.Texture(Key.minecraft(input == null ? legacyPath : path), false, false, true);
        }
    }

    private static JavaModel.Element cube(JavaModel.Texture texture,
                                          int textureSize,
                                          float u,
                                          float v,
                                          float x,
                                          float y,
                                          float z,
                                          float width,
                                          float height,
                                          float depth,
                                          JavaModel.Rotation rotation) {
        Map<Direction, JavaModel.Face> faces = new EnumMap<>(Direction.class);
        float unit = 16f / textureSize;
        faces.put(Direction.NORTH, face(texture, unit, u + depth, v + depth, u + depth + width, v + depth + height));
        faces.put(Direction.SOUTH, face(texture, unit, u + depth * 2 + width, v + depth, u + depth * 2 + width * 2, v + depth + height));
        faces.put(Direction.EAST, face(texture, unit, u, v + depth, u + depth, v + depth + height));
        faces.put(Direction.WEST, face(texture, unit, u + depth + width, v + depth, u + depth * 2 + width, v + depth + height));
        faces.put(Direction.UP, face(texture, unit, u + depth + width, v + depth, u + depth, v));
        faces.put(Direction.DOWN, face(texture, unit, u + depth + width * 2, v + depth, u + depth + width, v));
        return new JavaModel.Element(new Vector3f(x, y, z), new Vector3f(x + width, y + height, z + depth), faces, rotation);
    }

    private static JavaModel.Face face(JavaModel.Texture texture, float unit, float u1, float v1, float u2, float v2) {
        return new JavaModel.Face(texture, new JavaModel.Uv(u1 * unit, v1 * unit, u2 * unit, v2 * unit), 0, -1);
    }
}
