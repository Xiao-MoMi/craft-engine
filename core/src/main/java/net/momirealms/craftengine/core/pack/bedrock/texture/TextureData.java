package net.momirealms.craftengine.core.pack.bedrock.texture;

import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

public record TextureData(BufferedImage image, @Nullable TextureAnimation animation) {
}
