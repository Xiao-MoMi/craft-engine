package net.momirealms.craftengine.core.pack.bedrock.texture;

import net.momirealms.craftengine.core.util.ImageUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;

public record TextureData(BufferedImage image, @Nullable TextureAnimation animation, @Nullable String vanillaPath) {

    static final TextureData MISSING = missingTexture();

    public TextureData(BufferedImage image, @Nullable TextureAnimation animation) {
        this(image, animation, null);
    }

    private static TextureData missingTexture() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                image.setRGB(x, y, ((x < 8) == (y < 8)) ? 0xfff800f8 : 0xff000000);
            }
        }
        return new TextureData(image, null);
    }

    public boolean isMissing() {
        return this == MISSING;
    }

    public void forEachFrame(int color, TextureAnimation.FrameConsumer consumer) throws IOException {
        if (this.animation == null) {
            consumer.accept(0, color == 0xffffff || this.isMissing() ? this.image : ImageUtils.multiplyColor(this.image, color));
        } else {
            this.animation.forEachFrame(color, consumer);
        }
    }

    @Nullable
    public BedrockTexture staticTexture(int color) {
        if (this.animation != null) {
            return null;
        }
        return color == 0xffffff || this.isMissing()
                ? new BedrockTexture(this.image, this.vanillaPath)
                : new BedrockTexture(ImageUtils.multiplyColor(this.image, color));
    }
}
