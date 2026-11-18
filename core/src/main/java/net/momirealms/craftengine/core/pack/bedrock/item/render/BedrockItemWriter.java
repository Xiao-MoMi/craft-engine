package net.momirealms.craftengine.core.pack.bedrock.item.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemDisplayBlocks;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTexture;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.List;

public final class BedrockItemWriter {
    private final JavaModelResources resources;
    private final BedrockItemIconRenderer renderer;
    private final BedrockItemContextWriter contextWriter;
    private final BedrockItemVisualWriter visuals;

    public BedrockItemWriter(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        this.resources = resources;
        this.renderer = renderer;
        this.visuals = new BedrockItemVisualWriter(resources, renderer);
        this.contextWriter = new BedrockItemContextWriter(resources, renderer, this.visuals);
    }

    public List<GeneratedItemVariant> write(ResolvedItemBinding entry, BedrockItemGenerationContext context) throws IOException {
        BedrockItemPresentation.Plan plan = BedrockItemPresentation.plan(entry, this.resources.models());
        if (plan.contextual()) {
            return this.contextWriter.write(entry, plan, context);
        }

        // 资源分类仍按整个定义决定；注册分支、编号和闲置规则已在规划阶段选定
        ItemVisualType type = ItemVisualType.classify(entry.definition(), this.renderer);
        List<GeneratedItemVariant> variants = new ObjectArrayList<>(plan.variants().size());
        for (BedrockItemPresentation presentation : plan.variants()) {
            try {
                GeneratedItemVariant variant = this.writeVariant(presentation, context, type);
                this.visuals.writeWorldAliases(variant, false, context, variants);
                variants.add(variant);
            } catch (BedrockResourceException exception) {
                ItemBindingRule rule = presentation.rulesByContext().get(ItemDisplayContext.GUI).getFirst();
                TranslationManager.warn("resource_pack.bedrock.item.variant_skipped", presentation.id().asString(), entry.itemModel().asString(), rule.sourcePath(), exception.getLocalizedMessage());
            }
        }
        return variants;
    }

    private GeneratedItemVariant writeVariant(BedrockItemPresentation presentation,
                                              BedrockItemGenerationContext context,
                                              ItemVisualType type) throws IOException {
        ItemBindingRule rule = presentation.rulesByContext().get(ItemDisplayContext.GUI).getFirst();
        List<ItemBindingRule> parts = BedrockItemVisualWriter.modelParts(rule);
        if (parts.isEmpty()) {
            return BedrockItemVisualWriter.transparent(presentation.id(), presentation.conditions(), presentation.idleRules(), context);
        }
        return switch (type) {
            case DISPLAY_BLOCK -> this.writeDisplayBlock(presentation, context, rule, parts);
            case ICON -> this.writeIcon(presentation, context, parts);
        };
    }

    private GeneratedItemVariant writeDisplayBlock(BedrockItemPresentation presentation,
                                                   BedrockItemGenerationContext context,
                                                   ItemBindingRule rule,
                                                   List<ItemBindingRule> parts) throws IOException {
        Key id = presentation.id();
        BedrockItemDisplayBlocks.DisplayBlock display = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(), context.root());
        boolean attachable = BedrockAnimatedGeneratedItemAttachables.write(id, rule, this.resources.models(), this.resources.textures(), context.root());
        ItemBehaviorDescription behavior = presentation.item().clientBehavior();
        if (attachable && behavior.use().animation() == ItemBehaviorDescription.UseAnimation.NONE
                && behavior.attack().animation() == ItemBehaviorDescription.SwingAnimation.WHACK
                && behavior.interact().animation() == ItemBehaviorDescription.SwingAnimation.WHACK) {
            context.playerAnimations().registerItemUsingJavaSwing(id);
        }
        // 注册条件已在规划阶段回退和求交；资源写出后直接构造最终结果，不再用 GUI 条件中转。
        return new GeneratedItemVariant(id, presentation.conditions(), null, display, attachable, null, presentation.idleRules(), null, null);
    }

    private GeneratedItemVariant writeIcon(BedrockItemPresentation presentation,
                                           BedrockItemGenerationContext context,
                                           List<ItemBindingRule> parts) throws IOException {
        BedrockTexture icon = this.visuals.renderIcon(parts, false);
        Key id = presentation.id();
        String texture = BedrockItemVisualWriter.writeIconTexture(id, icon, context);
        JavaModel model = ((ItemRenderTarget.Model) parts.getFirst().target()).resolve(this.resources.models());
        boolean attachable = this.renderer.needsHandheldAttachable(model);
        if (attachable) {
            BedrockItemAttachables.write(id, model, icon.image(), texture, context.root());
        }
        return new GeneratedItemVariant(id, presentation.conditions(), texture, null, attachable, null, presentation.idleRules(), null, null);
    }
}
