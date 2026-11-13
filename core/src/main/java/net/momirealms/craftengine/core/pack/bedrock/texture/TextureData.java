package net.momirealms.craftengine.core.pack.bedrock.texture;

import net.momirealms.craftengine.core.util.ImageUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;

public record TextureData(BufferedImage image, @Nullable TextureAnimation animation, @Nullable String vanillaPath) {

    public TextureData(BufferedImage image, @Nullable TextureAnimation animation) {
        this(image, animation, null);
    }

    public void forEachFrame(int color, TextureAnimation.FrameConsumer consumer) throws IOException {
        if (this.animation == null) {
            consumer.accept(0, color == 0xffffff ? this.image : ImageUtils.multiplyColor(this.image, color));
        } else {
            this.animation.forEachFrame(color, consumer);
        }
    }

    @Nullable
    public BedrockTexture staticTexture(int color) {
        if (this.animation != null) {
            return null;
        }
        return color == 0xffffff
                ? new BedrockTexture(this.image, this.vanillaPath)
                : new BedrockTexture(ImageUtils.multiplyColor(this.image, color));
    }
}
