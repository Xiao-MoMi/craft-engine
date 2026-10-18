package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureAnimation;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.PngOptimizer;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GeneratedItemModelBaker {
    private static final String[] LAYERS = {"layer0", "layer1", "layer2", "layer3", "layer4"};
    private static final float MIN_Z = 7.5f;
    private static final float MAX_Z = 8.5f;
    private static final float UV_INSET = 0.1f;
    private static final float LAYER_OFFSET = 1f / 1024;
    private static final JavaModel.Uv FRONT_UV = new JavaModel.Uv(0, 0, 16, 16);
    private static final JavaModel.Uv BACK_UV = new JavaModel.Uv(16, 0, 0, 16);

    private GeneratedItemModelBaker() {
    }

    static Result bake(JavaModel model, BedrockTextureAtlas.TextureSource source, List<Integer> tints) throws IOException {
        List<BufferedImage> layers = new ArrayList<>(LAYERS.length);
        Map<BedrockTextureAtlas.TextureId, BedrockTextureAtlas.TextureData> originals = new HashMap<>();
        boolean animated = false;
        int width = 1;
        int height = 1;
        for (int index = 0; index < LAYERS.length; index++) {
            JavaModel.Texture texture = model.textures().get(LAYERS[index]);
            if (texture == null) {
                break;
            }
            BedrockTextureAtlas.TextureId id = BedrockTextureAtlas.TextureId.of(texture);
            BedrockTextureAtlas.TextureData data = originals.get(id);
            if (data == null) {
                data = source.load(id);
                if (data == null || data.image() == null) {
                    throw new IOException("Missing generated sprite " + texture.sprite() + " in model " + model.id());
                }
                originals.put(id, data);
            }
            animated |= data.animation() != null;
            BufferedImage image = data.image();
            layers.add(image);
            width = Math.max(width, image.getWidth());
            height = Math.max(height, image.getHeight());
        }
        if (animated) {
            return bakeAnimated(model, originals, tints);
        }
        for (int index = 0; index < layers.size() && index < tints.size(); index++) {
            int tint = tints.get(index) & 0xFFFFFF;
            if (tint != 0xFFFFFF) {
                layers.set(index, BedrockTextureAtlas.tint(layers.get(index), tint));
            }
        }
        // Flatten the static layers before extrusion so Bedrock never receives overlapping coplanar faces.
        BufferedImage image = composite(layers, width, height);
        JavaModel.Texture texture = new JavaModel.Texture(model.id(), null, false);
        List<JavaModel.Element> elements = new ArrayList<>();
        if (!layers.isEmpty()) {
            elements.add(new JavaModel.Element(new Vector3f(0, 0, MIN_Z), new Vector3f(16, 16, MAX_Z),
                    Map.of(Direction.SOUTH, face(texture, FRONT_UV), Direction.NORTH, face(texture, BACK_UV)), null, true, null, 0));
            addSides(elements, texture, image);
        }
        JavaModel baked = new JavaModel(model.id(), JavaModel.GeometryKind.ELEMENTS, elements, model.textures(),
                model.display(), model.ambientOcclusion(), model.guiLight());
        BedrockTextureAtlas atlas = new BedrockTextureAtlas(image, Map.of(BedrockTextureAtlas.TextureId.of(texture),
                new BedrockTextureAtlas.Region(0, 0, width, height)));
        return new Result(baked, atlas);
    }

    private static Result bakeAnimated(JavaModel model, Map<BedrockTextureAtlas.TextureId, BedrockTextureAtlas.TextureData> textures,
                                       List<Integer> tints) throws IOException {
        List<JavaModel.Element> elements = new ArrayList<>();
        for (int index = 0; index < LAYERS.length; index++) {
            JavaModel.Texture texture = model.textures().get(LAYERS[index]);
            if (texture == null) {
                break;
            }
            BedrockTextureAtlas.TextureData data = textures.get(BedrockTextureAtlas.TextureId.of(texture));
            // Keep layer timelines independent. A small depth separation preserves layer order without coplanar faces.
            float minZ = MIN_Z - index * LAYER_OFFSET;
            float maxZ = MAX_Z + index * LAYER_OFFSET;
            elements.add(new JavaModel.Element(new Vector3f(0, 0, minZ), new Vector3f(16, 16, maxZ),
                    Map.of(Direction.SOUTH, face(texture, FRONT_UV, index), Direction.NORTH, face(texture, BACK_UV, index)), null, true, null, 0));
            addSides(elements, texture, data.image(), data.animation(), index, minZ, maxZ);
        }
        JavaModel baked = new JavaModel(model.id(), JavaModel.GeometryKind.ELEMENTS, elements, model.textures(),
                model.display(), model.ambientOcclusion(), model.guiLight());
        BedrockTextureAtlas atlas = BedrockTextureAtlas.create(baked, new BedrockTextureAtlas.TextureSource() {
            @Override
            public BufferedImage read(BedrockTextureAtlas.TextureId texture) {
                return textures.get(texture).image();
            }

            @Override
            public BedrockTextureAtlas.TextureData load(BedrockTextureAtlas.TextureId texture) {
                return textures.get(texture);
            }
        }, tints);
        return new Result(baked, atlas);
    }

    private static BufferedImage composite(List<BufferedImage> layers, int width, int height) {
        if (layers.size() == 1) {
            return layers.getFirst();
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            for (BufferedImage layer : layers) {
                graphics.drawImage(layer, 0, 0, width, height, null);
            }
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static void addSides(List<JavaModel.Element> elements, JavaModel.Texture texture, BufferedImage image) {
        addSides(elements, texture, image, null, -1, MIN_Z, MAX_Z);
    }

    private static void addSides(List<JavaModel.Element> elements, JavaModel.Texture texture, BufferedImage image,
                                 @Nullable TextureAnimation animation, int tintIndex, float minZ, float maxZ) {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] edges = new byte[width * height];
        if (animation == null) {
            collectEdges(image, edges);
        } else {
            // Java's generated mesh retains the edges from every used frame, including pixels absent from the first frame.
            BitSet visited = new BitSet();
            int columns = animation.sheet().getWidth() / width;
            int[] pixels = new int[edges.length];
            for (TextureAnimation.Frame frame : animation.frames()) {
                if (!visited.get(frame.index())) {
                    visited.set(frame.index());
                    animation.sheet().getRGB(frame.index() % columns * width, frame.index() / columns * height, width, height, pixels, 0, width);
                    collectEdges(pixels, width, height, edges);
                }
            }
        }
        float scaleX = 16f / width;
        float scaleY = 16f / height;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int edge = edges[y * width + x];
                if (edge == 0) {
                    continue;
                }
                float minX = x * scaleX;
                float maxX = (x + 1) * scaleX;
                float minY = 16 - (y + 1) * scaleY;
                float maxY = 16 - y * scaleY;
                JavaModel.Uv uv = new JavaModel.Uv((x + UV_INSET) * scaleX, (y + UV_INSET) * scaleY,
                        (x + 1 - UV_INSET) * scaleX, (y + 1 - UV_INSET) * scaleY);
                JavaModel.Face face = face(texture, uv, tintIndex);
                if ((edge & 1) != 0) {
                    addSide(elements, face, Direction.UP, minX, maxY, maxX, maxY, minZ, maxZ);
                }
                if ((edge & 2) != 0) {
                    addSide(elements, face, Direction.DOWN, minX, minY, maxX, minY, minZ, maxZ);
                }
                if ((edge & 4) != 0) {
                    addSide(elements, face, Direction.WEST, minX, minY, minX, maxY, minZ, maxZ);
                }
                if ((edge & 8) != 0) {
                    addSide(elements, face, Direction.EAST, maxX, minY, maxX, maxY, minZ, maxZ);
                }
            }
        }
    }

    private static void collectEdges(BufferedImage image, byte[] edges) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels;
        if (image.getType() == BufferedImage.TYPE_INT_ARGB && image.getRaster().getParent() == null
                && image.getRaster().getDataBuffer() instanceof DataBufferInt buffer && buffer.getData().length == width * height) {
            pixels = buffer.getData();
        } else {
            pixels = new int[width * height];
            PngOptimizer.readPixels(image, pixels, 0, width);
        }
        collectEdges(pixels, width, height, edges);
    }

    private static void collectEdges(int[] pixels, int width, int height, byte[] edges) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if ((pixels[index] >>> 24) == 0) {
                    continue;
                }
                if (y == 0 || (pixels[index - width] >>> 24) == 0) {
                    edges[index] |= 1;
                }
                if (y == height - 1 || (pixels[index + width] >>> 24) == 0) {
                    edges[index] |= 2;
                }
                if (x == 0 || (pixels[index - 1] >>> 24) == 0) {
                    edges[index] |= 4;
                }
                if (x == width - 1 || (pixels[index + 1] >>> 24) == 0) {
                    edges[index] |= 8;
                }
            }
        }
    }

    private static void addSide(List<JavaModel.Element> elements, JavaModel.Face face, Direction direction,
                                float minX, float minY, float maxX, float maxY, float minZ, float maxZ) {
        elements.add(new JavaModel.Element(new Vector3f(minX, minY, minZ), new Vector3f(maxX, maxY, maxZ),
                Map.of(direction, face), null, true, null, 0));
    }

    private static JavaModel.Face face(JavaModel.Texture texture, JavaModel.Uv uv) {
        return face(texture, uv, -1);
    }

    private static JavaModel.Face face(JavaModel.Texture texture, JavaModel.Uv uv, int tintIndex) {
        return new JavaModel.Face(texture, uv, 0, null, tintIndex);
    }

    record Result(JavaModel model, BedrockTextureAtlas atlas) {
    }
}
