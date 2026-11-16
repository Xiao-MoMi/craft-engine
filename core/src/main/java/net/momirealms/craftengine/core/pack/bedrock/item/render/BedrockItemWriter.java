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

    public BedrockItemWriter(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        this.resources = resources;
        this.renderer = renderer;
        this.contextWriter = new BedrockItemContextWriter(resources, renderer);
    }

    public List<GeneratedItemVariant> write(ResolvedItemBinding entry, BedrockItemGenerationContext context) throws IOException {
        BedrockItemPresentation.Plan plan = BedrockItemPresentation.plan(entry, this.resources.models());
        if (plan.contextual()) {
            return this.contextWriter.write(entry, plan.variants(), context);
        }

        // 资源分类仍按整个定义决定；注册分支、编号和闲置规则已在规划阶段选定。
        ItemVisualType type = ItemVisualType.classify(entry.definition(), this.renderer);
        List<GeneratedItemVariant> variants = new ObjectArrayList<>(plan.variants().size());
        for (BedrockItemPresentation presentation : plan.variants()) {
            ItemBindingRule rule = presentation.rulesByContext().get(ItemDisplayContext.GUI).getFirst();
            Key id = presentation.id();
            try {
                GeneratedItemVariant variant = this.writeVariant(presentation, context, type);
                this.contextWriter.writeWorldAliases(variant, false, context, variants);
                variants.add(variant);
            } catch (BedrockResourceException exception) {
                TranslationManager.warn("resource_pack.bedrock.item.variant_skipped", id.asString(), entry.itemModel().asString(), rule.sourcePath(), exception.getLocalizedMessage());
            }
        }
        return variants;
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    private GeneratedItemVariant writeVariant(BedrockItemPresentation presentation,
                                              BedrockItemGenerationContext context,
                                              ItemVisualType type) throws IOException {
        Key id = presentation.id();
        ItemBindingRule rule = presentation.rulesByContext().get(ItemDisplayContext.GUI).getFirst();
        List<ItemBindingRule> parts;
        if (rule.target() == ItemRenderTarget.Builtin.EMPTY) {
            return BedrockItemContextWriter.transparent(id, presentation.conditions(), presentation.idleRules(), context);
        } else if (rule.target() instanceof ItemRenderTarget.Model) {
            parts = List.of(rule);
        } else if (rule.target() instanceof ItemRenderTarget.Composite composite) {
            parts = composite.parts();
        } else {
            throw new BedrockResourceException("resource_pack.bedrock.reason.render_target", rule.target().toString());
        }
        if (parts.isEmpty()) {
            return BedrockItemContextWriter.transparent(id, presentation.conditions(), presentation.idleRules(), context);
        }
        for (ItemBindingRule part : parts) {
            if (!(part.target() instanceof ItemRenderTarget.Model)) {
                throw new BedrockResourceException("resource_pack.bedrock.reason.composite_child", part.sourcePath());
            }
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
        Key id = presentation.id();
        List<BedrockTexture> images = new ObjectArrayList<>(parts.size());
        for (ItemBindingRule part : parts) {
            BedrockTexture image = this.renderer.render((ItemRenderTarget.Model) part.target());
            if (image == null) {
                throw new BedrockResourceException("resource_pack.bedrock.reason.static_icon", part.sourcePath());
            }
            images.add(image);
        }
        BedrockTexture icon = BedrockTexture.composite(images);
        String texture = icon.write(context.root(), "textures/" + id.namespace() + '/' + id.value() + "/icon");
        JavaModel model = ((ItemRenderTarget.Model) parts.getFirst().target()).resolve(this.resources.models());
        boolean attachable = this.renderer.needsHandheldAttachable(model);
        if (attachable) {
            BedrockItemAttachables.write(id, model, icon.image(), texture, context.root());
        }
        return new GeneratedItemVariant(id, presentation.conditions(), texture, null, attachable, null, presentation.idleRules(), null, null);
    }
}
