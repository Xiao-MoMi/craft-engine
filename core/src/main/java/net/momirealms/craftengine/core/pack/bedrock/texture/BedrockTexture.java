package net.momirealms.craftengine.core.pack.bedrock.texture;

import net.momirealms.craftengine.core.util.ImageUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public record BedrockTexture(BufferedImage image, @Nullable String reference) {

    public BedrockTexture(BufferedImage image) {
        this(image, null);
    }

    public String write(Path root, String path) throws IOException {
        if (this.reference != null) {
            return this.reference;
        }
        ImageUtils.writePng(this.image, root.resolve(path + ".png"));
        return path;
    }

    public static BedrockTexture composite(List<BedrockTexture> layers) {
        if (layers.size() == 1) {
            return layers.getFirst();
        }
        return new BedrockTexture(ImageUtils.composite(layers, BedrockTexture::image));
    }
}
