package net.momirealms.craftengine.core.pack.bedrock.texture;

import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 区分模型使用的贴图及其染色版本。同一贴图使用不同颜色时，会占用不同的图集区域。
 *
 * @param sprite Java 图集中的贴图标识，不一定等于实际 PNG 路径
 * @param atlas 显式指定的 Java 图集；为 null 时由数据源选择默认图集
 * @param color RGB 染色值，0xFFFFFF 表示不染色
 */
public record TextureId(Key sprite, @Nullable Key atlas, int color) {

    public TextureId(Key sprite, @Nullable Key atlas) {
        this(sprite, atlas, 0xFFFFFF);
    }

    /**
     * 根据模型面的 tintIndex 选择颜色；未指定或超出列表范围时不染色。
     */
    public static TextureId of(JavaModel.Face face, List<Integer> tints) {
        int index = face.tintIndex();
        int color = index >= 0 && index < tints.size() ? tints.get(index) & 0xFFFFFF : 0xFFFFFF;
        return new TextureId(face.texture().sprite(), face.texture().atlas(), color);
    }

    /**
     * 创建不染色的贴图标识，用于读取原始图片。
     */
    public static TextureId of(JavaModel.Texture texture) {
        return new TextureId(texture.sprite(), texture.atlas());
    }
}
