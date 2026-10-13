package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.pack.model.generation.GuiLight;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record JavaModel(Key id, GeometryKind geometryKind, List<Element> elements,
                        Map<String, Texture> textures, Map<DisplayPosition, Transform> display,
                        boolean ambientOcclusion, GuiLight guiLight) {

    public JavaModel {
        elements = List.copyOf(elements);
        textures = Map.copyOf(textures);
        display = Map.copyOf(display);
    }

    public enum GeometryKind {
        ELEMENTS, GENERATED, ENTITY
    }

    public record Vector(float x, float y, float z) {
        public static final Vector ZERO = new Vector(0, 0, 0);
        public static final Vector ONE = new Vector(1, 1, 1);
    }

    public record Uv(float u1, float v1, float u2, float v2) {
    }

    public record Texture(Key sprite, @Nullable Key atlas, boolean forceTranslucent) {
    }

    public record Face(Texture texture, Uv uv, int rotation, @Nullable Direction cullFace, int tintIndex) {
    }

    public record Rotation(Vector origin, Vector angles, @Nullable Direction.Axis axis, boolean rescale) {
    }

    public record Element(Vector from, Vector to, Map<Direction, Face> faces,
                          @Nullable Rotation rotation, boolean shade, @Nullable Direction shadeDirectionOverride, int lightEmission) {

        public Element {
            faces = Map.copyOf(faces);
        }
    }

    public record Transform(Vector rotation, Vector translation, Vector scale) {
        public static final Transform IDENTITY = new Transform(Vector.ZERO, Vector.ZERO, Vector.ONE);
    }
}
