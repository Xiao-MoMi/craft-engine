package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemDefinitionBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.GeometryKind;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel.Texture;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureData;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;

import java.io.IOException;
import java.util.List;

enum ItemRenderType {
    ITEM, BLOCK, UNKNOWN;

    // 根据模型变换、三维几何和动态贴图确定默认渲染方式。
    static ItemRenderType classify(ItemDefinitionBinding definition, JavaModelResources resources) throws IOException {
        return classify(definition.rules(), resources);
    }

    private static ItemRenderType classify(List<ItemBindingRule> rules, JavaModelResources resources) throws IOException {
        boolean block = false;
        boolean unknown = false;
        // 任意分支含模型变换、三维模型或动态贴图，整个定义都使用展示方块。
        for (ItemBindingRule rule : rules) {
            block |= !rule.transformations().isEmpty();
            switch (rule.target()) {
                case ItemRenderTarget.Model model -> {
                    JavaModel resolved = model.resolve(resources.models());
                    if (resolved.kind() != GeometryKind.GENERATED) {
                        block = true;
                    } else {
                        for (Texture layer : resources.models().resolveLayers(resolved)) {
                            TextureData data = resources.textures().read(layer, TextureUsage.ITEM);
                            block |= data.animation() != null;
                        }
                    }
                }
                case ItemRenderTarget.Special ignored -> block = true;
                case ItemRenderTarget.Composite composite -> {
                    ItemRenderType type = classify(composite.parts(), resources);
                    block |= type == BLOCK;
                    unknown |= type == UNKNOWN;
                }
                case ItemRenderTarget.Builtin builtin -> unknown |= builtin != ItemRenderTarget.Builtin.EMPTY;
            }
        }
        return block ? BLOCK : (unknown ? UNKNOWN : ITEM);
    }
}
