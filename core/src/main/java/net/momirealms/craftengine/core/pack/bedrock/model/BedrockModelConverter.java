package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.util.Direction;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BedrockModelConverter {

    public Result convert(JavaModel model, String identifier, BedrockTextureAtlas.TextureSource textures) throws IOException {
        if (model.geometryKind() != JavaModel.GeometryKind.ELEMENTS) {
            throw new IllegalArgumentException("Static cube conversion requires ELEMENTS, got " + model.geometryKind() + " for " + model.id());
        }
        if (!identifier.startsWith("geometry.") || identifier.length() == "geometry.".length()) {
            throw new IllegalArgumentException("Expected a geometry identifier: " + identifier);
        }
        BedrockTextureAtlas atlas = BedrockTextureAtlas.create(model, textures);
        List<BedrockGeometry.Cube> cubes = new ArrayList<>(model.elements().size());
        for (JavaModel.Element element : model.elements()) {
            cubes.add(this.convertElement(element, atlas));
        }
        BedrockGeometry geometry = new BedrockGeometry(identifier, atlas.image().getWidth(), atlas.image().getHeight(), cubes, BedrockModelTransform.bounds(cubes));
        return new Result(model, geometry, atlas);
    }

    private BedrockGeometry.Cube convertElement(JavaModel.Element element, BedrockTextureAtlas atlas) {
        Vector3f from = element.from();
        Vector3f to = element.to();
        JavaModel.Rotation rotation = element.rotation();
        BedrockGeometry.Rotation convertedRotation = null;
        if (rotation != null) {
            if (rotation.rescale()) {
                Vector3f scale = BedrockModelTransform.rescale(rotation.angles());
                from = BedrockModelTransform.scaleAround(from, rotation.origin(), scale);
                to = BedrockModelTransform.scaleAround(to, rotation.origin(), scale);
            }
            Vector3f angles = rotation.angles();
            if (angles.x() != 0 || angles.y() != 0 || angles.z() != 0) {
                convertedRotation = new BedrockGeometry.Rotation(BedrockModelTransform.position(rotation.origin()),
                        new Vector3f(-angles.x(), -angles.y(), angles.z()));
            }
        }
        // Blockbench: center X/Z by -8, then compileCube reflects X using -(from.x + size.x).
        Vector3f origin = new Vector3f(8 - to.x(), from.y(), from.z() - 8);
        Vector3f size = new Vector3f(to.x() - from.x(), to.y() - from.y(), to.z() - from.z());
        Map<Direction, BedrockGeometry.Face> faces = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, JavaModel.Face> entry : element.faces().entrySet()) {
            JavaModel.Face face = entry.getValue();
            faces.put(entry.getKey(), this.convertFace(entry.getKey(), face, atlas.region(face.texture())));
        }
        return new BedrockGeometry.Cube(origin, size, convertedRotation, faces);
    }

    private BedrockGeometry.Face convertFace(Direction direction, JavaModel.Face face, BedrockTextureAtlas.Region region) {
        JavaModel.Uv uv = face.uv();
        float u = uv.u1();
        float v = uv.v1();
        float width = uv.u2() - u;
        float height = uv.v2() - v;
        // Blockbench compileCube reverses both UV dimensions on the up/down faces.
        if (direction.axis() == Direction.Axis.Y) {
            u = uv.u2();
            v = uv.v2();
            width = -width;
            height = -height;
        }
        float scaleU = region.width() / 16.0f;
        float scaleV = region.height() / 16.0f;
        return new BedrockGeometry.Face(region.x() + u * scaleU, region.y() + v * scaleV, width * scaleU, height * scaleV, face.rotation());
    }

    public record Result(JavaModel source, BedrockGeometry geometry, BedrockTextureAtlas atlas) {
    }
}
