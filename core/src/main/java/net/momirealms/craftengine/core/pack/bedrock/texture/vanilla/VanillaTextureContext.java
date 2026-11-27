package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaTextureAnimation;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ClientPngReader;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

record VanillaTextureContext(JavaResourcePack pack, Path root, VanillaTextureWriter output) {

    boolean changedTexture(String source) {
        String path = ResourcePackUtils.vanillaTexturePath(source);
        return this.pack.hasCustomResource(path) || this.pack.hasCustomResource(path + ".mcmeta");
    }

    TextureData load(String source) throws IOException {
        return this.loadResource(ResourcePackUtils.vanillaTexturePath(source));
    }

    TextureData loadResource(String path) throws IOException {
        try (InputStream input = this.pack.openResource(path)) {
            if (input == null) {
                throw new IOException("Missing texture " + path);
            }
            return JavaTextureAnimation.read(
                    ClientPngReader.read(input),
                    this.pack.readResourceMetadata(path),
                    this.pack.version().isAtOrAbove(MinecraftVersion.V1_21_11)
            );
        }
    }

    void transform(String output, List<String> sources,
                   Function<List<BufferedImage>, BufferedImage> transform) {
        this.transform(output, sources, false, transform);
    }

    void transform(String output, List<String> sources,
                   boolean changed,
                   Function<List<BufferedImage>, BufferedImage> transform) {
        if (!changed) {
            for (String source : sources) {
                if (this.changedTexture(source)) {
                    changed = true;
                    break;
                }
            }
            if (!changed) {
                return;
            }
        }
        try {
            List<TextureData> inputs = new ObjectArrayList<>(sources.size());
            for (String source : sources) {
                inputs.add(this.load(source));
            }
            this.output.writeFrames(String.join(", ", sources), VanillaTextureMappings.INSTANCE.target(output), inputs, transform);
        } catch (IOException | RuntimeException exception) {
            CraftEngine.instance().logger().warn("Skipping Bedrock vanilla texture " + output + ": " + exception.getMessage());
        }
    }
}
