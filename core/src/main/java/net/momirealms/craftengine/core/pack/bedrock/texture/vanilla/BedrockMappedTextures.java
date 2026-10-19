package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.BedrockConversionContext;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaTextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class BedrockMappedTextures implements BedrockTextureConverter {
    private final List<BedrockTextureRule> rules;

    BedrockMappedTextures(List<BedrockTextureRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public void convert(BedrockConversionContext context) {
        for (BedrockTextureRule rule : this.rules) {
            String source = rule.source();
            if (!context.pack().hasCustomResource(ResourcePackUtils.vanillaTexturePath(source))) {
                continue;
            }
            int firstTarget = 0;
            while (firstTarget < rule.targets().size() && context.textures().contains(rule.targets().get(firstTarget).path())) {
                firstTarget++;
            }
            if (firstTarget == rule.targets().size()) {
                continue;
            }
            try (InputStream input = context.pack().openCustomResource(ResourcePackUtils.vanillaTexturePath(source))) {
                JsonObject metadata = context.pack().readResourceMetadata(ResourcePackUtils.vanillaTexturePath(source));
                boolean animated = metadata != null && metadata.has("animation");
                byte[] bytes = input.readAllBytes();
                TextureData data = null;
                BufferedImage image = null;
                for (int index = firstTarget; index < rule.targets().size(); index++) {
                    BedrockTextureRule.Target target = rule.targets().get(index);
                    if (context.textures().contains(target.path())) {
                        continue;
                    }
                    if (!animated) {
                        if (context.textures().claim(target.path(), source)) {
                            BedrockTextureRule.Tga tga = BedrockVanillaTextures.tga(target.path());
                            if (tga != null) {
                                if (image == null) {
                                    image = ClientPngReader.read(new ByteArrayInputStream(bytes));
                                }
                                ImageUtils.writeTga(image, context.root().resolve(tga.path()));
                                if (!tga.keep()) {
                                    continue;
                                }
                            }
                            Path file = context.root().resolve(target.path());
                            Files.createDirectories(file.getParent());
                            Files.write(file, bytes);
                            if (target.firstFrame() != null && context.textures().claim(target.firstFrame(), source)) {
                                Path firstFrame = context.root().resolve(target.firstFrame());
                                Files.createDirectories(firstFrame.getParent());
                                Files.write(firstFrame, bytes);
                            }
                        }
                    } else {
                        if (data == null) {
                            data = JavaTextureAnimation.read(ClientPngReader.read(new ByteArrayInputStream(bytes)), metadata,
                                    context.pack().version().isAtOrAbove(MinecraftVersion.V1_21_11));
                        }
                        // 图集使用播放序列的首帧；flipbook 单独读取完整条带，不能把条带作为图集底图。
                        if (target.firstFrame() != null) {
                            BedrockVanillaTextures.writeImage(context, source, target.firstFrame(), data.image());
                        }
                        if (!BedrockVanillaTextures.writeKeyFrames(context, source, target, data.animation())) {
                            BedrockVanillaTextures.writeFrames(context, source, target, List.of(data), List::getFirst);
                        }
                    }
                    // 静态替换也覆盖原版 flipbook，避免客户端继续用原版动画刷新这个图集条目。
                    if (target.firstFrame() != null && (!animated || data.animation() == null)) {
                        String path = target.path().substring(0, target.path().lastIndexOf('.'));
                        for (BedrockTextureRule.Tile tile : target.tiles()) {
                            context.textures().flipbook(tile.name(), tile.index(), path, 1, tile.replicate(), List.of(0), false);
                        }
                    }
                }
            } catch (IOException | IllegalArgumentException | ArithmeticException exception) {
                CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + source + ": " + exception.getMessage());
            }
        }
    }
}
