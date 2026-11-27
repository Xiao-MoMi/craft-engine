package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaTextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

final class BedrockMappedTextures implements BedrockTextureConverter {
    private final List<BedrockTextureRule> rules;

    BedrockMappedTextures(List<BedrockTextureRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public void convert(VanillaTextureContext context) {
        for (BedrockTextureRule rule : this.rules) {
            String source = rule.source();
            if (!context.changedTexture(source)) {
                continue;
            }
            int firstTarget = 0;
            while (firstTarget < rule.targets().size() && !context.output().needsMappedTexture(rule.targets().get(firstTarget).path())) {
                firstTarget++;
            }
            if (firstTarget == rule.targets().size()) {
                continue;
            }
            String path = ResourcePackUtils.vanillaTexturePath(source);
            try (InputStream input = context.pack().openResource(path)) {
                JsonObject metadata = context.pack().readResourceMetadata(path);
                if (metadata == null || !metadata.has("animation")) {
                    context.output().copyMappedTextures(source, rule.targets(), firstTarget, input.readAllBytes());
                    continue;
                }

                TextureData data = JavaTextureAnimation.read(ClientPngReader.read(input), metadata,
                        context.pack().version().isAtOrAbove(MinecraftVersion.V1_21_11));
                for (int index = firstTarget; index < rule.targets().size(); index++) {
                    BedrockTextureRule.Target target = rule.targets().get(index);
                    if (context.output().needsMappedTexture(target.path())) {
                        context.output().writeMappedTexture(source, target, data);
                    }
                }
            } catch (IOException | RuntimeException exception) {
                CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + source + ": " + exception.getMessage());
            }
        }
    }
}
