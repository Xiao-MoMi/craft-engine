package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

final class BedrockLayerTextures implements BedrockTextureConverter {
    private static final List<LayerRule> RULES = readRules();

    private static List<LayerRule> readRules() {
        JsonArray entries = GsonHelper.readJsonFromResource("/bedrock/vanilla_texture_layers.json").getAsJsonArray();
        List<LayerRule> rules = new ObjectArrayList<>(entries.size());
        for (JsonElement entry : entries) {
            JsonObject rule = entry.getAsJsonObject();
            JsonArray layerEntries = rule.getAsJsonArray("layers");
            List<Layer> layers = new ObjectArrayList<>(layerEntries.size());
            List<String> sources = new ObjectArrayList<>(layerEntries.size());
            for (JsonElement layerEntry : layerEntries) {
                JsonObject layer = layerEntry.getAsJsonObject();
                String source = layer.get("source").getAsString();
                Integer color = layer.has("color") ? layer.get("color").getAsInt() : null;
                Key itemTint = layer.has("item_tint") ? Key.of(layer.get("item_tint").getAsString()) : null;
                int tintIndex = layer.has("tint_index") ? layer.get("tint_index").getAsInt() : 0;
                layers.add(new Layer(source, color, itemTint, tintIndex));
                sources.add(source);
            }
            boolean mask = rule.has("mask") && rule.get("mask").getAsBoolean();
            boolean emptyOnly = mask && rule.get("empty_only").getAsBoolean();
            rules.add(new LayerRule(rule.get("target").getAsString(), List.copyOf(layers), List.copyOf(sources), mask, emptyOnly));
        }
        return List.copyOf(rules);
    }

    @Override
    public void convert(VanillaTextureContext context) {
        JavaItemTintSource itemTints = new JavaItemTintSource(context.pack());
        for (LayerRule rule : RULES) {
            List<Layer> layers = rule.layers();
            boolean changed = rule.sources().stream().anyMatch(context::changedTexture);
            for (Layer layer : layers) {
                if (layer.itemTint() != null) {
                    changed |= context.pack().hasCustomResource(ResourcePackUtils.itemDefinitionPath(layer.itemTint()))
                            || context.pack().hasCustomResource("assets/minecraft/textures/colormap/grass.png");
                }
            }
            if (!changed) {
                continue;
            }
            try {
                int[] colors = new int[layers.size()];
                for (int index = 0; index < layers.size(); index++) {
                    Layer layer = layers.get(index);
                    if (layer.itemTint() != null) {
                        Integer tint = itemTints.read(layer.itemTint(), layer.tintIndex());
                        if (tint == null) {
                            throw new IOException("Missing item tint definition " + layer.itemTint());
                        }
                        colors[index] = tint;
                    }
                }
                context.transform(rule.target(), rule.sources(), true,
                        images -> this.combine(rule, images, colors));
            } catch (IOException | RuntimeException exception) {
                CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + rule.target() + ": " + exception.getMessage());
            }
        }
    }

    private BufferedImage combine(LayerRule rule, List<BufferedImage> images, int[] colors) {
        int width = images.stream().mapToInt(BufferedImage::getWidth).max().orElseThrow();
        int height = images.stream().mapToInt(BufferedImage::getHeight).max().orElseThrow();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            List<Layer> layers = rule.layers();
            for (int index = 0; index < images.size(); index++) {
                BufferedImage image = images.get(index);
                Layer layer = layers.get(index);
                if (layer.itemTint() != null) {
                    image = ImageUtils.multiplyColor(image, colors[index]);
                } else if (layer.color() != null) {
                    image = ImageUtils.tint(image, layer.color());
                }
                // mask=true 时，将后续图层的完全不透明像素写为 alpha=2，供对应基岩版材质识别染色或发光区域。
                if (index > 0 && rule.mask()) {
                    if (image.getWidth() != width || image.getHeight() != height) {
                        image = ImageUtils.resize(image, width, height);
                    }
                    // empty_only=true 只填充结果中完全透明的位置；false 允许覆盖已有像素。
                    boolean emptyOnly = rule.emptyOnly();
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

    private record LayerRule(String target,
                             List<Layer> layers,
                             List<String> sources,
                             boolean mask,
                             boolean emptyOnly) {
    }

    private record Layer(String source,
                         @Nullable Integer color,
                         @Nullable Key itemTint,
                         int tintIndex) {
    }
}
