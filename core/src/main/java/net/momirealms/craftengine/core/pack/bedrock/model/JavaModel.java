package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.pack.model.generation.GuiLight;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public record JavaModel(Key id,
                        GeometryKind geometryKind,
                        List<Element> elements,
                        Map<String, Texture> textures,
                        Map<DisplayPosition, Transform> display,
                        boolean ambientOcclusion,
                        GuiLight guiLight) {

    public JavaModel {
        elements = List.copyOf(elements);
        textures = Map.copyOf(textures);
        display = Map.copyOf(display);
    }

    public enum GeometryKind {
        ELEMENTS, GENERATED, ENTITY
    }

    public record Uv(float u1, float v1, float u2, float v2) {
    }

    public record Texture(Key sprite, @Nullable Key atlas, boolean forceTranslucent) {
    }

    public record Face(Texture texture, Uv uv, int rotation, @Nullable Direction cullFace, int tintIndex) {
    }

    public record Rotation(Vector3f origin, Vector3f angles, @Nullable Direction.Axis axis, boolean rescale) {
    }

    public record Element(Vector3f from,
                          Vector3f to,
                          Map<Direction, Face> faces,
                          @Nullable Rotation rotation,
                          boolean shade,
                          @Nullable Direction shadeDirectionOverride,
                          int lightEmission) {

        public Element {
            faces = Map.copyOf(faces);
        }
    }

    public record Transform(Vector3f rotation, Vector3f translation, Vector3f scale) {
        private static final Transform IDENTITY = new Transform(VectorUtils.ZERO, VectorUtils.ZERO, VectorUtils.ONE);

        public static Transform identity() {
            return IDENTITY;
        }
    }
}
