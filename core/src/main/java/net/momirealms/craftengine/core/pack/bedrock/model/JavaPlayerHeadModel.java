package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class JavaPlayerHeadModel {

    private JavaPlayerHeadModel() {
    }

    static JavaModel create(JavaModel base, Key skin, boolean legacyPose) {
        // 26.1 起，原版物品 JSON 自己平移并翻转头颅；此前由特殊渲染器完成这一步。
        float center = legacyPose ? 8 : 0;
        JavaModel.Rotation rotation = new JavaModel.Rotation(new Vector3f(center, 0, center),
                legacyPose ? new Vector3f(0, 180, 0) : new Vector3f(0, 0, 180), false);
        JavaModel.Texture texture = new JavaModel.Texture(skin, false, true);
        List<JavaModel.Element> elements = List.of(layer(texture, center, rotation, false), layer(texture, center, rotation, true));
        return new JavaModel(base.id(), JavaModel.GeometryKind.ELEMENTS, Map.of(), elements, base.display(), base.guiLight());
    }

    private static JavaModel.Element layer(JavaModel.Texture texture, float center, JavaModel.Rotation rotation, boolean hat) {
        float padding = hat ? 0.25f : 0;
        float u = hat ? 8 : 0;
        Map<Direction, JavaModel.Face> faces = new EnumMap<>(Direction.class);
        faces.put(Direction.NORTH, face(texture, u + 2, 2, u + 4, 4));
        faces.put(Direction.SOUTH, face(texture, u + 6, 2, u + 8, 4));
        faces.put(Direction.EAST, face(texture, u, 2, u + 2, 4));
        faces.put(Direction.WEST, face(texture, u + 4, 2, u + 6, 4));
        faces.put(Direction.UP, face(texture, u + 4, 2, u + 2, 0));
        faces.put(Direction.DOWN, face(texture, u + 6, 2, u + 4, 0));
        return new JavaModel.Element(new Vector3f(center - 4 - padding, -padding, center - 4 - padding),
                new Vector3f(center + 4 + padding, 8 + padding, center + 4 + padding), faces, rotation);
    }

    private static JavaModel.Face face(JavaModel.Texture texture, float u1, float v1, float u2, float v2) {
        return new JavaModel.Face(texture, new JavaModel.Uv(u1, v1, u2, v2), 0, -1);
    }
}
