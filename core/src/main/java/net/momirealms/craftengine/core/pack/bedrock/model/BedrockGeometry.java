package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.util.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public record BedrockGeometry(String identifier, int textureWidth, int textureHeight, List<Cube> cubes, Bounds bounds) {

    public BedrockGeometry {
        cubes = List.copyOf(cubes);
    }

    public record Face(float u, float v, float width, float height, int rotation) {
    }

    public record Rotation(Vector3f pivot, Vector3f angles) {
    }

    public record Bounds(float width, float height, Vector3f offset) {
    }

    public record Bone(String name, @Nullable String parent, Vector3f pivot, @Nullable String binding, List<Cube> cubes) {

        public Bone {
            cubes = List.copyOf(cubes);
        }
    }

    public record Cube(Vector3f origin, Vector3f size, @Nullable Rotation rotation, Map<Direction, Face> faces) {

        public Cube {
            faces = Map.copyOf(faces);
        }
    }
}
