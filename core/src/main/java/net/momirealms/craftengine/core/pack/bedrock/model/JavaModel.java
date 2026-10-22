package net.momirealms.craftengine.core.pack.bedrock.model;

import com.google.gson.JsonElement;
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
                        GeometryKind kind,
                        Map<String, JsonElement> textures,
                        List<Element> elements,
                        Map<DisplayPosition, Transform> display,
                        GuiLight guiLight) {

    public enum GeometryKind {
        GENERATED, ELEMENTS, ENTITY
    }

    public record Element(Vector3f from, Vector3f to, Map<Direction, Face> faces, @Nullable Rotation rotation) {
    }

    public record Face(Texture texture, Uv uv, int rotation, int tintIndex) {
    }

    public record Texture(Key sprite, boolean forceTranslucent) {
    }

    public record Uv(float u1, float v1, float u2, float v2) {
    }

    public record Rotation(Vector3f origin, Vector3f angles, boolean rescale) {
    }

    public record Transform(Vector3f rotation, Vector3f translation, Vector3f scale) {
        public static final Transform IDENTITY = new Transform(VectorUtils.ZERO, VectorUtils.ZERO, VectorUtils.ONE);
    }
}
