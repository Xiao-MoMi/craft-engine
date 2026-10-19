package net.momirealms.craftengine.core.pack.bedrock.texture;

import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * 一次贴图加载的结果。
 *
 * @param image 静态显示用的图片；存在动画时为播放序列的首帧
 * @param animation 完整动画数据，静态贴图为 null
 */
public record TextureData(BufferedImage image, @Nullable TextureAnimation animation) {
}
