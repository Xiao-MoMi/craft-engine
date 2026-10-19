package net.momirealms.craftengine.core.pack.bedrock.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 按贴图标识读取图片及动画数据，供模型转换和图集生成使用。
 */
@FunctionalInterface
public interface TextureSource {

    /**
     * 读取用于静态显示的图片；动画贴图通常返回播放序列的首帧。
     */
    BufferedImage read(TextureId texture) throws IOException;

    /**
     * 默认按静态贴图加载；支持动画的数据源应重写此方法，保留动画信息。
     */
    default TextureData load(TextureId texture) throws IOException {
        return new TextureData(this.read(texture), null);
    }
}
