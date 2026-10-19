package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.Region;
import net.momirealms.craftengine.core.pack.model.generation.GuiLight;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.awt.image.BufferedImage;
import java.util.List;

public final class JavaItemIconRenderer {
    public static final int DEFAULT_SIZE = 64;
    private static final float RADIANS = (float) (Math.PI / 180);
    private static final Direction[] DIRECTIONS = Direction.values();
    // Java FaceInfo vertex order, encoded as X/Y/Z maximum-coordinate bits.
    private static final int[][] CORNERS = {
            {4, 0, 1, 5}, {2, 6, 7, 3}, {3, 1, 0, 2},
            {6, 4, 5, 7}, {2, 0, 4, 6}, {7, 5, 1, 3}
    };
    private static final Vector3fc[] FRONT_LIGHTS = lights(new Matrix4f().rotationY(-22.5f * RADIANS).rotateX(135 * RADIANS));
    private static final Vector3fc[] SIDE_LIGHTS = lights(new Matrix4f().scaling(1, -1, 1)
            .rotateYXZ(62 * RADIANS, 185.5f * RADIANS, 0).rotateYXZ(-22.5f * RADIANS, 135 * RADIANS, 0));
    private final ItemIconRasterizer rasterizer;
    private final float center;
    private final float pixelScale;
    private final ItemIconRasterizer.Vertex[] vertices = {
            new ItemIconRasterizer.Vertex(), new ItemIconRasterizer.Vertex(),
            new ItemIconRasterizer.Vertex(), new ItemIconRasterizer.Vertex()
    };
    private final Matrix4f gui = new Matrix4f();
    private final Matrix4f elementTransform = new Matrix4f();
    private final Vector3f point = new Vector3f();
    private final Vector3f normal = new Vector3f();
    private final Vector3f edge = new Vector3f();

    public JavaItemIconRenderer() {
        this(DEFAULT_SIZE);
    }

    public JavaItemIconRenderer(int size) {
        this.rasterizer = new ItemIconRasterizer(size);
        this.center = size / 2f;
        this.pixelScale = size / 16f;
    }

    private static Vector3fc[] lights(Matrix4f transform) {
        return new Vector3fc[]{
                transform.transformDirection(new Vector3f(0.2f, 1, -0.7f).normalize()),
                transform.transformDirection(new Vector3f(-0.2f, 1, 0.7f).normalize())
        };
    }

    public BufferedImage render(JavaModel model, BedrockTextureAtlas atlas, List<Integer> tints) {
        if (model.geometryKind() != JavaModel.GeometryKind.ELEMENTS) {
            throw new IllegalArgumentException("GUI cube rendering requires ELEMENTS: " + model.id());
        }
        BufferedImage image = this.rasterizer.begin();
        JavaModel.Transform display = model.display().getOrDefault(DisplayPosition.GUI, JavaModel.Transform.identity());
        Vector3f angles = display.rotation();
        // Java ItemTransform applies translation, XYZ rotation, scale, then centers the model.
        this.gui.translation(display.translation()).rotateXYZ(angles.x() * RADIANS, angles.y() * RADIANS, angles.z() * RADIANS)
                .scale(display.scale()).translate(-8, -8, -8);
        Vector3fc[] lights = model.guiLight() == GuiLight.FRONT ? FRONT_LIGHTS : SIDE_LIGHTS;
        for (JavaModel.Element element : model.elements()) {
            this.elementTransform.set(this.gui);
            JavaModel.Rotation rotation = element.rotation();
            if (rotation != null) {
                Vector3f pivot = rotation.origin();
                Vector3f rotationAngles = rotation.angles();
                this.elementTransform.translate(pivot)
                        .rotateXYZ(rotationAngles.x() * RADIANS, rotationAngles.y() * RADIANS, rotationAngles.z() * RADIANS);
                if (rotation.rescale()) {
                    this.elementTransform.scale(BedrockModelTransform.rescale(rotationAngles));
                }
                this.elementTransform.translate(-pivot.x(), -pivot.y(), -pivot.z());
            }
            for (Direction direction : DIRECTIONS) {
                JavaModel.Face face = element.faces().get(direction);
                if (face == null) {
                    continue;
                }
                this.project(element, face, direction);
                float light = this.light(lights);
                Region region = atlas.region(face, tints);
                this.rasterizer.triangle(this.vertices[0], this.vertices[1], this.vertices[2], atlas.image(), region, light);
                this.rasterizer.triangle(this.vertices[0], this.vertices[2], this.vertices[3], atlas.image(), region, light);
            }
        }
        return image;
    }

    private void project(JavaModel.Element element, JavaModel.Face face, Direction direction) {
        Vector3f from = element.from();
        Vector3f to = element.to();
        int[] corners = CORNERS[direction.ordinal()];
        JavaModel.Uv uv = face.uv();
        for (int index = 0; index < 4; index++) {
            int corner = corners[index];
            this.point.set((corner & 1) == 0 ? from.x() : to.x(), (corner & 2) == 0 ? from.y() : to.y(),
                    (corner & 4) == 0 ? from.z() : to.z());
            this.elementTransform.transformPosition(this.point);
            ItemIconRasterizer.Vertex vertex = this.vertices[index];
            vertex.x = this.center + this.point.x() * this.pixelScale;
            vertex.y = this.center - this.point.y() * this.pixelScale;
            vertex.z = this.point.z();
            int uvCorner = (index + face.rotation() / 90) % 4;
            vertex.u = uvCorner < 2 ? uv.u1() : uv.u2();
            vertex.v = uvCorner == 0 || uvCorner == 3 ? uv.v1() : uv.v2();
        }
    }

    private float light(Vector3fc[] lights) {
        ItemIconRasterizer.Vertex a = this.vertices[0];
        ItemIconRasterizer.Vertex b = this.vertices[1];
        ItemIconRasterizer.Vertex c = this.vertices[2];
        this.normal.set((b.x - a.x) / this.pixelScale, (a.y - b.y) / this.pixelScale, b.z - a.z);
        this.edge.set((c.x - a.x) / this.pixelScale, (a.y - c.y) / this.pixelScale, c.z - a.z);
        this.normal.cross(this.edge);
        if (this.normal.lengthSquared() == 0) {
            return 1;
        }
        this.normal.normalize();
        this.normal.y = -this.normal.y;
        // Java's item light shader combines two directional lights with 0.4 ambient and 0.6 diffuse.
        return Math.min(1, 0.4f + 0.6f * (Math.max(0, this.normal.dot(lights[0])) + Math.max(0, this.normal.dot(lights[1]))));
    }
}
