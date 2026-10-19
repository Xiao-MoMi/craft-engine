package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BedrockParticleTextures implements BedrockTextureConverter {

    @Override
    public void convert(BedrockConversionContext context) {
        Map<BedrockParticleDefinition, List<Source>> targets = new LinkedHashMap<>();
        for (BedrockParticleRule rule : BedrockParticleRules.ALL) {
            Source source = this.read(context, rule);
            if (source == null) {
                continue;
            }
            if (rule.unsupportedReason() != null) {
                if (source.changed()) {
                    CraftEngine.instance().logger().warn("Keeping original Bedrock particle for " + rule.source() + ": " + rule.unsupportedReason());
                }
                continue;
            }
            for (BedrockParticleDefinition target : rule.targets()) {
                targets.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
            }
        }
        Map<String, TextureData> loaded = new HashMap<>();
        for (Map.Entry<BedrockParticleDefinition, List<Source>> entry : targets.entrySet()) {
            List<Source> sources = entry.getValue();
            if (sources.stream().noneMatch(Source::changed)) {
                continue;
            }
            BedrockParticleDefinition target = entry.getKey();
            Source first = sources.getFirst();
            // 同一网络效果无法区分多个 Java 来源。即使只改了其中一个，也不能覆盖另一个的材质。
            if (sources.stream().anyMatch(source -> !first.compatible(source))) {
                CraftEngine.instance().logger().warn("Keeping original Bedrock particle " + target.identifier()
                        + ": incompatible Java sources " + sources.stream().map(source -> source.rule().source()).toList());
                continue;
            }
            if (first.paths().isEmpty()) {
                continue;
            }
            try {
                List<BufferedImage> frames = new ArrayList<>(first.paths().size());
                for (String path : first.paths()) {
                    TextureData data = loaded.get(path);
                    if (data == null) {
                        data = BedrockVanillaTextures.loadResource(context, path);
                        loaded.put(path, data);
                    }
                    if (data.animation() != null) {
                        // Java 图集动画使用全局时钟，不能悄悄改成从每个粒子出生时开始播放。
                        throw new IOException("animated sprite " + path + " needs a global atlas animation clock; particle-age playback is not equivalent");
                    }
                    frames.add(data.image());
                }
                BufferedImage image = this.stitch(frames, first.rule());
                String texture = "textures/particle/craftengine/" + target.file();
                JsonObject definition = target.create(texture, first.rule().selection(), frames.size());
                if (!BedrockVanillaTextures.writeImage(context, first.rule().source(), texture + ".png", image)) {
                    continue;
                }
                Path file = context.root().resolve("particles/" + target.file() + ".json");
                Files.createDirectories(file.getParent());
                GsonHelper.writeJsonFile(definition, file);
            } catch (IOException | IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
                CraftEngine.instance().logger().warn("Keeping original Bedrock particle " + target.identifier() + ": " + exception.getMessage());
            }
        }
    }

    private Source read(BedrockConversionContext context, BedrockParticleRule rule) {
        String definitionPath = "assets/minecraft/particles/" + rule.source() + ".json";
        try {
            JsonObject definition = context.pack().readJson(definitionPath);
            if (definition == null) {
                return null;
            }
            if (!(definition.get("textures") instanceof JsonArray sequence) || sequence.isEmpty()) {
                throw new IOException("Missing particle texture sequence");
            }
            boolean changed = context.pack().hasCustomResource(definitionPath);
            List<String> paths = new ArrayList<>(sequence.size());
            for (JsonElement entry : sequence) {
                Key texture = Key.of(entry.getAsString());
                String path = ResourcePackUtils.texturePath(new Key(texture.namespace(), "particle/" + texture.value()));
                paths.add(path);
                changed |= context.pack().hasCustomResource(path) || context.pack().hasCustomResource(path + ".mcmeta");
                if (rule.selection() == BedrockParticleRule.Selection.QUARTER) {
                    // SulfurCubeProvider 只使用 SpriteSet.first()，后面的纹理不参与选帧。
                    break;
                }
            }
            // 不去重：重复帧也影响播放时长或随机选中的概率。
            return new Source(rule, paths, changed);
        } catch (IOException | IllegalArgumentException | IllegalStateException | JsonParseException exception) {
            CraftEngine.instance().logger().warn("Skipping Java particle " + rule.source() + ": " + exception.getMessage());
            // 保留失败来源参与共用目标检查，不能让另一条规则绕过冲突直接覆盖。
            return new Source(rule, List.of(), true);
        }
    }

    private BufferedImage stitch(List<BufferedImage> frames, BedrockParticleRule rule) {
        int width = frames.stream().mapToInt(BufferedImage::getWidth).max().orElseThrow();
        int height = frames.stream().mapToInt(BufferedImage::getHeight).max().orElseThrow();
        // Java 的 5 像素树叶在基岩版 7 像素格子内留一圈透明边；高清图片按比例放大边距。
        int borderX = Math.round(width * rule.padding() / 5.0f);
        int borderY = Math.round(height * rule.padding() / 5.0f);
        int cellWidth = Math.addExact(width, borderX * 2);
        int cellHeight = Math.addExact(height, borderY * 2);
        BufferedImage result = new BufferedImage(cellWidth, Math.multiplyExact(cellHeight, frames.size()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        try {
            for (int index = 0; index < frames.size(); index++) {
                if (rule.selection() == BedrockParticleRule.Selection.QUARTER) {
                    // BreakingItemParticle 的 U0 > U1，随机裁片之前先保留这次水平翻转。
                    graphics.drawImage(frames.get(index), width, index * cellHeight, -width, height, null);
                } else {
                    graphics.drawImage(frames.get(index), borderX, index * cellHeight + borderY, width, height, null);
                }
            }
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private record Source(BedrockParticleRule rule, List<String> paths, boolean changed) {

        boolean compatible(Source other) {
            return this.paths.equals(other.paths) && this.rule.selection() == other.rule.selection() && this.rule.padding() == other.rule.padding();
        }
    }
}
