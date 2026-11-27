package net.momirealms.craftengine.core.pack.bedrock.item.visual;

import net.momirealms.craftengine.bedrock.BedrockTintSource;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;

import java.io.IOException;
import java.util.List;

public final class BedrockItemTint {
    public static final int MULTIPLE = -5;
    private int source = BedrockTintSource.NONE;
    private Integer defaultColor;

    public static BedrockItemTint analyze(List<ItemBindingRule> rules, JavaModelResources resources) throws IOException {
        BedrockItemTint tint = new BedrockItemTint();
        tint.collect(rules, resources);
        return tint;
    }

    public int source() {
        return this.source;
    }

    public Integer defaultColor() {
        return this.dynamic() ? this.defaultColor : null;
    }

    public boolean dynamic() {
        return BedrockTintSource.isDynamic(this.source);
    }

    private void collect(List<ItemBindingRule> rules, JavaModelResources resources) throws IOException {
        for (ItemBindingRule rule : rules) {
            if (rule.target() instanceof ItemRenderTarget.Composite composite) {
                this.collect(composite.parts(), resources);
            } else if (rule.target() instanceof ItemRenderTarget.Model target) {
                JavaModel model = target.resolve(resources.models());
                if (model.kind() == JavaModel.GeometryKind.GENERATED) {
                    int layers = resources.models().resolveLayers(model).size();
                    for (int index = 0; index < layers; index++) {
                        this.collect(target, index, resources);
                    }
                } else {
                    for (JavaModel.Element element : model.elements()) {
                        for (JavaModel.Face face : element.faces().values()) {
                            this.collect(target, face.tintIndex(), resources);
                        }
                    }
                }
            }
        }
    }

    private void collect(ItemRenderTarget.Model model, int index, JavaModelResources resources) throws IOException {
        if (index < 0 || index >= model.tints().size()) {
            return;
        }
        Tint tint = model.tints().get(index);
        int source = JavaPackTextureSource.tintSource(tint);
        if (source == BedrockTintSource.NONE) {
            return;
        }
        if (this.source == BedrockTintSource.NONE) {
            this.source = source;
            this.defaultColor = resources.textures().resolveTint(tint);
        } else if (this.source != source) {
            this.source = MULTIPLE;
        }
    }
}
