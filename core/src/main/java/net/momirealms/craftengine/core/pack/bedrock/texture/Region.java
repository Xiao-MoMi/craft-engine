package net.momirealms.craftengine.core.pack.bedrock.texture;

/**
 * 贴图在生成图集中的像素区域，不包含为防止边缘串色而复制的边框。
 *
 * @param x 左上角横坐标，单位为像素
 * @param y 左上角纵坐标，单位为像素
 * @param width 贴图宽度，单位为像素
 * @param height 贴图高度，单位为像素
 */
public record Region(int x, int y, int width, int height) {
}
