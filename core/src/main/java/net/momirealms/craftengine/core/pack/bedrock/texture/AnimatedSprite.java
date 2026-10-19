package net.momirealms.craftengine.core.pack.bedrock.texture;

/**
 * 模型图集中需要单独播放动画的一张贴图。
 *
 * @param material 动画对应的材质标识，用于关联模型面与动画贴图
 * @param animation 原始帧图、播放序列及插值设置
 * @param color 渲染每帧时乘上的 RGB 颜色，0xFFFFFF 表示不染色
 */
public record AnimatedSprite(String material, TextureAnimation animation, int color) {
}
