package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockBlockItemModel;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelConverter;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.PngOptimizer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public final class BedrockBlockItems {
    private final JsonObject textures = new JsonObject();
    private final JsonObject blocks = new JsonObject();
    private final JsonArray flipbooks = new JsonArray();

    public BedrockPackMappings.DisplayBlock write(BedrockModelConverter.Result converted, Key resource, Path root) throws IOException {
        return this.write(converted, resource, root, List.of());
    }

    public BedrockPackMappings.DisplayBlock write(BedrockModelConverter.Result converted, Key resource, Path root,
                                                List<BedrockAnimatedTextures.Track> animations) throws IOException {
        return this.write(converted, resource, root, animations, true);
    }

    public BedrockPackMappings.DisplayBlock write(BedrockModelConverter.Result converted, Key resource, Path root,
                                                List<BedrockAnimatedTextures.Track> animations, boolean held) throws IOException {
        String name = "ce_item_" + HashUtils.sha1(resource.asString().getBytes(StandardCharsets.UTF_8));
        String identifier = "geometry." + name;
        BufferedImage image = converted.atlas().image();
        int textureWidth = Math.max(image.getWidth(), image.getHeight());
        int textureHeight = animations.isEmpty() ? image.getHeight() : textureWidth;
        JsonObject geometry = BedrockBlockItemModel.convert(converted, identifier, textureWidth, textureHeight, held);
        Path path = root.resolve("models/blocks/" + name + ".geo.json");
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(geometry, path);
        String texturePath = "textures/" + resource.namespace() + '/' + resource.value();
        if (textureWidth != image.getWidth() || textureHeight != image.getHeight()) {
            // Terrain treats tall textures as animation strips. Pad the canvas while retaining pixel UVs and the held texture.
            BufferedImage padded = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
            int[] pixels = ((DataBufferInt) padded.getRaster().getDataBuffer()).getData();
            PngOptimizer.readPixels(image, pixels, 0, textureWidth);
            texturePath = "textures/ce_blocks/" + name;
            image = padded;
        }
        Path textureFile = root.resolve(texturePath + ".png");
        Files.createDirectories(textureFile.getParent());
        try (OutputStream output = Files.newOutputStream(textureFile)) {
            new PngOptimizer(image, 0).write(output);
        }
        JsonObject texture = new JsonObject();
        texture.addProperty("textures", texturePath);
        this.textures.add(name, texture);
        JsonObject block = new JsonObject();
        block.addProperty("sound", "wood");
        this.blocks.add("geyser_custom:" + name, block);
        Map<String, String> materials = animations.isEmpty() ? Map.of() : new LinkedHashMap<>();
        for (BedrockAnimatedTextures.Track animation : animations) {
            JsonObject animatedTexture = new JsonObject();
            animatedTexture.addProperty("textures", animation.strip());
            this.textures.add(animation.texture(), animatedTexture);
            materials.put(animation.material(), animation.texture());
            JsonObject flipbook = new JsonObject();
            flipbook.addProperty("atlas_tile", animation.texture());
            flipbook.addProperty("flipbook_texture", animation.strip());
            flipbook.addProperty("ticks_per_frame", animation.interval());
            // Interpolation is baked at Java tick boundaries, including per-frame durations and alpha behavior.
            flipbook.addProperty("blend_frames", false);
            this.flipbooks.add(flipbook);
        }
        return new BedrockPackMappings.DisplayBlock(name, identifier, name, materials);
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

    public void writeAtlas(Path root) throws IOException {
        if (this.isEmpty()) {
            return;
        }
        JsonObject atlas = new JsonObject();
        atlas.addProperty("resource_pack_name", "craftengine");
        atlas.addProperty("texture_name", "atlas.terrain");
        atlas.addProperty("padding", 8);
        atlas.addProperty("num_mip_levels", 4);
        atlas.add("texture_data", this.textures);
        Path path = root.resolve("textures/terrain_texture.json");
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(atlas, path);
        if (!this.flipbooks.isEmpty()) {
            GsonHelper.writeJsonFile(this.flipbooks, root.resolve("textures/flipbook_textures.json"));
        }
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(1);
        version.add(0);
        this.blocks.add("format_version", version);
        GsonHelper.writeJsonFile(this.blocks, root.resolve("blocks.json"));
    }
}
