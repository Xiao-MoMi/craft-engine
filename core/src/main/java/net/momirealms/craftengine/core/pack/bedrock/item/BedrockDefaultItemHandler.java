package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemIcons.Variant;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.model.definition.Transformation;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public final class BedrockDefaultItemHandler implements BedrockItemHandler {
    private final JavaModelResolver models;
    private final JavaPackTextureSource textures;
    private final BedrockItemIconRenderer renderer;

    public BedrockDefaultItemHandler(JavaModelResolver models, JavaPackTextureSource textures, BedrockItemIconRenderer renderer) {
        this.models = models;
        this.textures = textures;
        this.renderer = renderer;
    }

    @Override
    public boolean write(ItemRegistrationPlan.Entry entry,
                         BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = entry.definition().rules();
        JavaModelResolver.RenderType type = this.models.classify(entry.definition());
        List<Variant> branches = new ObjectArrayList<>(rules.size());
        context.variants().put(entry.bedrockId(), branches);
        for (int i = 0; i < rules.size(); i++) {
            ItemBindingRule rule = rules.get(i);
            Key id;
            if (rules.size() == 1) { // 单分支复用物品 ID，多分支分别分配资源名
                id = entry.bedrockId();
            } else {
                id = Key.of(entry.bedrockId().namespace(), entry.bedrockId().value() + "_variant_" + i);
            }
            branches.add(new Variant(id, rule, null)); // 先保留规则，未生成资源的分支也会写入计划

            // TODO 特殊的模型类型
            if (!(rule.target() instanceof ItemRenderTarget.Model) && !(rule.target() instanceof ItemRenderTarget.Composite)) {
                continue;
            }
            try {
                List<ItemBindingRule> parts;
                if (rule.target() instanceof ItemRenderTarget.Composite) {
                    parts = new ObjectArrayList<>();
                    this.collectModels(rule, new ObjectArrayList<>(), parts);
                } else {
                    parts = List.of(rule);
                }
                JavaModel displayModel = this.sharedDisplayModel(parts);
                if (type == JavaModelResolver.RenderType.BLOCK) {
                    context.displays().put(id, context.blocks().write(id, parts, this.models, this.textures, context.root()));
                    continue;
                }
                if (type == JavaModelResolver.RenderType.ITEM) { // 无法确定渲染方式时不生成二维图标
                    List<BufferedImage> images = new ObjectArrayList<>(parts.size());
                    for (ItemBindingRule part : parts) {
                        BufferedImage image = this.renderer.render((ItemRenderTarget.Model) part.target());
                        if (image == null) {
                            throw new UnsupportedOperationException("Model cannot be rendered as a static icon: " + part.sourcePath());
                        }
                        images.add(image);
                    }
                    BufferedImage image = ImageUtils.composite(images);
                    String path = "textures/" + id.namespace() + '/' + id.value() + "/icon";
                    ImageUtils.writePng(image, context.root().resolve(path + ".png"));
                    JsonObject icon = new JsonObject();
                    icon.addProperty("textures", path); // 基岩图集路径不带 .png 后缀
                    context.icons().add(id.asString(), icon);
                    if (displayModel != null && this.models.needsHandheldAttachable(displayModel)) {
                        BedrockItemAttachables.write(id, displayModel, image, path, context.root());
                        context.attachables().add(id);
                    }
                }
            } catch (UnsupportedOperationException exception) { // 不支持的转换只记录当前分支的跳过原因
                branches.set(i, new Variant(id, rule, exception.getMessage()));
            }
        }
        return true;
    }

    private void collectModels(ItemBindingRule rule, List<Transformation> transformations, List<ItemBindingRule> parts) {
        int size = transformations.size();
        transformations.addAll(rule.transformations());
        switch (rule.target()) {
            case ItemRenderTarget.Model model -> parts.add(new ItemBindingRule(List.of(), model, transformations, rule.sourcePath()));
            case ItemRenderTarget.Composite composite -> {
                for (ItemBindingRule child : composite.children()) {
                    if (!child.conditions().isEmpty()) {
                        throw new UnsupportedOperationException("Conditions inside composite are not supported: " + child.sourcePath());
                    }
                    this.collectModels(child, transformations, parts);
                }
            }
            case ItemRenderTarget.Builtin builtin when builtin == ItemRenderTarget.Builtin.EMPTY -> {
            }
            default -> throw new UnsupportedOperationException("Unsupported composite child: " + rule.sourcePath());
        }
        // 只保留当前路径的父子变换，不能把前一个子模型的变换带入后一个。
        while (transformations.size() > size) {
            transformations.removeLast();
        }
    }

    @Nullable
    private JavaModel sharedDisplayModel(List<ItemBindingRule> parts) throws IOException {
        JavaModel first = null;
        for (ItemBindingRule part : parts) {
            JavaModel model = this.models.resolve(((ItemRenderTarget.Model) part.target()).model());
            if (first == null) {
                first = model;
            } else if (!first.display().equals(model.display())) {
                throw new UnsupportedOperationException("Composite models have different display transforms: " + first.id() + " and " + model.id());
            }
        }
        return first;
    }
}
