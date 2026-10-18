package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureAtlas;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTextureRegistry;
import net.momirealms.craftengine.core.util.ImageUtils;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class BedrockLayerTextures implements BedrockTextureConverter {
    private static final JsonArray RULES = BedrockVanillaTextures.readRules("vanilla_texture_layers.json").getAsJsonArray();

    @Override
    public void convert(JavaResourcePack pack, Path output, BedrockTextureRegistry textures) {
        JavaItemTintSource itemTints = new JavaItemTintSource(pack);
        for (JsonElement entry : RULES) {
            JsonObject rule = entry.getAsJsonObject();
            JsonArray layers = rule.getAsJsonArray("layers");
            List<String> sources = new ArrayList<>(layers.size());
            for (JsonElement layer : layers) {
                sources.add(layer.getAsJsonObject().get("source").getAsString());
            }
            String target = rule.get("target").getAsString();
            try {
                int[] colors = new int[layers.size()];
                boolean changed = false;
                boolean available = true;
                for (int index = 0; index < layers.size(); index++) {
                    JsonObject layer = layers.get(index).getAsJsonObject();
                    if (layer.has("item_tint")) {
                        JavaItemTintSource.Color color = itemTints.read(layer.get("item_tint").getAsString());
                        if (color == null) {
                            available = false;
                            break;
                        }
                        colors[index] = color.value();
                        changed |= color.changed();
                    }
                }
                if (available) {
                    BedrockVanillaTextures.transform(pack, output, textures, target, sources, changed, images -> this.combine(rule, images, colors));
                }
            } catch (IOException | IllegalArgumentException exception) {
                BedrockVanillaTextures.warn(target, exception);
            }
        }
    }

    private BufferedImage combine(JsonObject rule, List<BufferedImage> images, int[] colors) {
        int width = images.stream().mapToInt(BufferedImage::getWidth).max().orElseThrow();
        int height = images.stream().mapToInt(BufferedImage::getHeight).max().orElseThrow();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            JsonArray layers = rule.getAsJsonArray("layers");
            for (int index = 0; index < images.size(); index++) {
                BufferedImage image = images.get(index);
                JsonObject layer = layers.get(index).getAsJsonObject();
                if (layer.has("item_tint")) {
                    image = BedrockTextureAtlas.tint(image, colors[index]);
                } else if (layer.has("color")) {
                    image = ImageUtils.tint(image, layer.get("color").getAsInt());
                }
                if (index > 0 && rule.has("mask")) {
                    if (image.getWidth() != width || image.getHeight() != height) {
                        image = ImageUtils.resize(image, width, height);
                    }
                    boolean emptyOnly = rule.get("empty_only").getAsBoolean();
                    // Bedrock's affected TGA textures use low alpha to distinguish tint/emissive regions.
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int pixel = image.getRGB(x, y);
                            if ((pixel >>> 24) == 255 && (!emptyOnly || (result.getRGB(x, y) >>> 24) == 0)) {
                                result.setRGB(x, y, 0x02000000 | (pixel & 0xffffff));
                            }
                        }
                    }
                } else {
                    graphics.drawImage(image, 0, 0, width, height, null);
                }
            }
        } finally {
            graphics.dispose();
        }
        return result;
    }
}
