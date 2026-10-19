package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.core.item.setting.value.BedrockOptions;
import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockBlockItemModel;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockModelConverter;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockAnimatedTextures;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.PngOptimizer;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockBlockItems {
    private final BedrockConversionContext context;
    private final JsonObject blocks = new JsonObject();

    public BedrockBlockItems(BedrockConversionContext context) {
        this.context = context;
    }

    public BedrockPackMappings.DisplayBlock write(BedrockModelConverter.Result converted, Key resource) throws IOException {
        return this.write(converted, resource, List.of());
    }

    public BedrockPackMappings.DisplayBlock write(BedrockModelConverter.Result converted, Key resource,
                                                List<BedrockAnimatedTextures.Track> animations) throws IOException {
        return this.write(converted, resource, animations, true);
    }

    public BedrockPackMappings.DisplayBlock write(BedrockModelConverter.Result converted, Key resource,
                                                List<BedrockAnimatedTextures.Track> animations, boolean held) throws IOException {
        String name = "ce_item_" + HashUtils.sha1(resource.asString().getBytes(StandardCharsets.UTF_8));
        String identifier = "geometry." + name;
        BufferedImage image = converted.atlas().image();
        int textureWidth = Math.max(image.getWidth(), image.getHeight());
        int textureHeight = animations.isEmpty() ? image.getHeight() : textureWidth;
        JsonObject geometry = BedrockBlockItemModel.convert(converted, identifier, textureWidth, textureHeight, held);
        Path path = this.context.root().resolve("models/blocks/" + name + ".geo.json");
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
        Path textureFile = this.context.root().resolve(texturePath + ".png");
        Files.createDirectories(textureFile.getParent());
        try (OutputStream output = Files.newOutputStream(textureFile)) {
            new PngOptimizer(image, 0).write(output);
        }
        this.context.textures().terrain(name, texturePath);
        JsonObject block = new JsonObject();
        block.addProperty("sound", "wood");
        this.blocks.add("geyser_custom:" + name, block);
        Map<String, String> materials = animations.isEmpty() ? Map.of() : new LinkedHashMap<>();
        for (BedrockAnimatedTextures.Track animation : animations) {
            this.context.textures().terrain(animation.texture(), animation.strip());
            materials.put(animation.material(), animation.texture());
            this.context.textures().flipbook(animation.texture(), -1, animation.strip(), animation.interval());
        }
        return new BedrockPackMappings.DisplayBlock(name, identifier, name, materials);
    }

    public boolean isEmpty() {
        return this.blocks.isEmpty();
    }

    @Nullable
    public BedrockPackMappings.DisplayBlock applyOptions(@Nullable BedrockPackMappings.DisplayBlock display, BedrockOptions options) {
        if (display == null || display.renderMethod().equals(options.renderMethod())) {
            return display;
        }
        String name = display.name() + '_' + options.renderMethod();
        this.blocks.add("geyser_custom:" + name, this.blocks.get("geyser_custom:" + display.name()));
        return new BedrockPackMappings.DisplayBlock(name, display.geometry(), display.texture(), display.materials(), options.renderMethod());
    }

    public void writeBlocks() throws IOException {
        if (this.isEmpty()) {
            return;
        }
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(1);
        version.add(0);
        this.blocks.add("format_version", version);
        GsonHelper.writeJsonFile(this.blocks, this.context.root().resolve("blocks.json"));
    }
}
