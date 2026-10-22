package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Texture;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.ResourcePackUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import static net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource.WHITE;

public final class BedrockItemIcons {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;

    public BedrockItemIcons(JavaResourcePack pack) {
        this.textures = new JavaPackTextureSource(pack);
        this.models = new JavaModelResolver(pack, this.textures);
    }

    public Result write(ItemRegistrationPlan plan, Path root) throws IOException {
        JsonObject icons = new JsonObject(); // 图标资源键到 PNG 路径的映射
        Map<Key, BedrockItemBlocks.DisplayBlock> displays = new LinkedHashMap<>(); // 分支 ID 到展示方块资源
        Map<Key, List<Variant>> variants = new LinkedHashMap<>(); // 物品 ID 到全部显示分支
        BedrockItemBlocks blocks = new BedrockItemBlocks();
        for (ItemRegistrationPlan.Entry entry : plan.entries()) {
            try {
                JavaModelResolver.RenderType type = this.models.classify(entry.definition()); // 任一分支含三维或动态贴图，整个定义走方块渲染
                List<ItemBindingRule> rules = entry.definition().rules();
                List<Variant> branches = new ObjectArrayList<>(rules.size());
                variants.put(entry.bedrockId(), branches);
                for (int i = 0; i < rules.size(); i++) {
                    ItemBindingRule rule = rules.get(i);
                    Key id;
                    String iconName;
                    if (rules.size() == 1) { // 单分支复用物品 ID，多分支分别分配资源名
                        id = entry.bedrockId();
                        iconName = entry.icon();
                    } else {
                        id = Key.of(entry.bedrockId().namespace(), entry.bedrockId().value() + "_v" + i);
                        iconName = entry.icon() + "_v" + i;
                    }
                    branches.add(new Variant(id, iconName, rule, null)); // 先保留规则，未生成资源的分支也会写入计划

                    // TODO 组合渲染暂未实现，只跳过当前分支
                    if (rule.target() instanceof ItemRenderTarget.Composite) {
                        branches.set(i, new Variant(id, iconName, rule, "Composite rendering is not supported"));
                        continue;
                    }

                    // TODO 模型变换及特殊的模型类型
                    if (!rule.transformations().isEmpty() || !(rule.target() instanceof ItemRenderTarget.Model model)) {
                        continue;
                    }

                    if (type == JavaModelResolver.RenderType.BLOCK) {
                        try {
                            JavaModel resolved = this.models.resolve(model.model());
                            if (resolved.kind() == JavaModel.GeometryKind.GENERATED) {
                                List<Texture> textures = this.models.resolveLayers(resolved);
                                List<TextureData> layers = new ObjectArrayList<>(textures.size());
                                List<Integer> colors = new ObjectArrayList<>(textures.size());
                                for (Texture texture : textures) {
                                    int index = layers.size();
                                    Integer color = index < model.tints().size() ? this.textures.resolveTint(model.tints().get(index)) : WHITE;
                                    TextureData data = this.textures.read(texture, TextureUsage.ITEM);
                                    layers.add(data);
                                    colors.add(color);
                                }
                                displays.put(id, blocks.writeGenerated(id, resolved, layers, colors, root));
                            } else if (resolved.kind() == JavaModel.GeometryKind.ELEMENTS) {
                                // Java elements 转为基岩几何和面材质
                                displays.put(id, blocks.writeElements(id, resolved, model, this.textures, root));
                            }
                        } catch (UnsupportedOperationException exception) { // 不支持的转换只记录当前分支的跳过原因
                            branches.set(i, new Variant(id, iconName, rule, exception.getMessage()));
                        }
                    } else if (type == JavaModelResolver.RenderType.ITEM) { // 无法确定渲染方式时不生成二维图标
                        BufferedImage image = this.render(model); // 静态图层染色后合成；不支持时返回 null
                        if (image == null) {
                            continue;
                        }
                        String path = "textures/craftengine_icons/" + id.namespace() + '/' + id.value();
                        ImageUtils.writePng(image, root.resolve(path + ".png"));
                        JsonObject icon = new JsonObject();
                        icon.addProperty("textures", path); // 基岩图集路径不带 .png 后缀
                        icons.add(iconName, icon);
                    }
                }
            } catch (IOException | RuntimeException exception) { // 读取或生成失败时补充定义 ID、路径并终止
                throw new IOException("Failed to generate icon for " + entry.definitionId() + " at " + ResourcePackUtils.itemDefinitionPath(entry.definitionId()) + ": " + exception.getMessage(), exception);
            }
        }
        JsonObject atlas = new JsonObject(); // 汇总写出物品图集
        atlas.addProperty("resource_pack_name", "craftengine");
        atlas.addProperty("texture_name", "atlas.items");
        atlas.add("texture_data", icons);
        Path output = root.resolve(BedrockPackLayout.ITEM_TEXTURE_ATLAS);
        Files.createDirectories(output.getParent());
        GsonHelper.writeJsonFile(GsonHelper.sortObjectKeys(atlas), output);
        blocks.finish(root); // 汇总写出方块图集、flipbook 和方块定义
        return new Result(icons.keySet(), displays, variants); // 供注册计划关联已生成资源与分支条件
    }

    public record Result(Set<String> icons, Map<Key, BedrockItemBlocks.DisplayBlock> displayBlocks, Map<Key, List<Variant>> variants) {
    }

    public record Variant(Key id, String icon, ItemBindingRule rule, @Nullable String unsupportedReason) {
    }

    @Nullable
    private BufferedImage render(ItemRenderTarget.Model target) throws IOException {
        JavaModel model = this.models.resolve(target.model());
        List<Texture> textures = this.models.resolveIconLayers(model);
        if (textures == null || textures.isEmpty()) {
            return null;
        }
        List<BufferedImage> layers = new ArrayList<>(textures.size());
        for (int index = 0; index < textures.size(); index++) {
            Integer color = index < target.tints().size() ? this.textures.resolveTint(target.tints().get(index)) : WHITE;
            BufferedImage image = this.textures.readStatic(textures.get(index), TextureUsage.ITEM);
            if (image == null) {
                return null;
            }
            // 先按各层 tint 染色再合成，未染色的 overlay 保持原色。
            layers.add(color == 0xffffff ? image : ImageUtils.multiplyColor(image, color));
        }
        return ImageUtils.composite(layers);
    }
}
