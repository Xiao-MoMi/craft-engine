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
                GeneratedItemVariant variant = this.writeVariant(entry, context, id, rule, type);
                // 规划阶段已经处理未知条件回退，注册时不能重新使用原始规则中的条件。
                variant = new GeneratedItemVariant(
                        variant.id(),
                        presentation.conditions(),
                        variant.iconTexture(),
                        variant.displayBlock(),
                        variant.attachable(),
                        variant.dyeColor(),
                        presentation.idleRules(),
                        null,
                        null
                );
                this.contextWriter.writeWorldAliases(variant, false, context, variants);
                variants.add(variant);
            } catch (BedrockResourceException exception) {
                TranslationManager.warn("resource_pack.bedrock.item.variant_skipped", id.asString(), entry.itemModel().asString(), rule.sourcePath(), exception.getLocalizedMessage());
            }
        }
        return variants;
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    private GeneratedItemVariant writeVariant(ResolvedItemBinding entry,
                                              BedrockItemGenerationContext context,
                                              Key id,
                                              ItemBindingRule rule,
                                              ItemVisualType type) throws IOException {
        List<ItemBindingRule> parts;
        if (rule.target() == ItemRenderTarget.Builtin.EMPTY) {
            return BedrockItemContextWriter.transparent(id, rule.conditions(), context);
        } else if (rule.target() instanceof ItemRenderTarget.Model) {
            parts = List.of(rule);
        } else if (rule.target() instanceof ItemRenderTarget.Composite composite) {
            parts = composite.parts();
        } else {
            throw new BedrockResourceException("resource_pack.bedrock.reason.render_target", rule.target().toString());
        }
        if (parts.isEmpty()) {
            return BedrockItemContextWriter.transparent(id, rule.conditions(), context);
        }
        List<JavaModel> models = new ObjectArrayList<>(parts.size());
        for (ItemBindingRule part : parts) {
            // 没法支持 special 那些东西
            if (!(part.target() instanceof ItemRenderTarget.Model target)) {
                throw new BedrockResourceException("resource_pack.bedrock.reason.composite_child", part.sourcePath());
            }
            JavaModel model = target.resolve(this.resources.models());
            models.add(model);
        }
        return switch (type) {
            case DISPLAY_BLOCK -> this.writeDisplayBlock(entry, context, id, rule, parts);
            case ICON -> this.writeIcon(context, id, rule, parts, models);
        };
    }

    private GeneratedItemVariant writeDisplayBlock(ResolvedItemBinding entry,
                                                   BedrockItemGenerationContext context,
                                                   Key id,
                                                   ItemBindingRule rule,
                                                   List<ItemBindingRule> parts) throws IOException {
        BedrockItemDisplayBlocks.DisplayBlock display = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(), context.root());
        if (!BedrockAnimatedGeneratedItemAttachables.write(id, rule, this.resources.models(), this.resources.textures(), context.root())) {
            return GeneratedItemVariant.block(id, rule.conditions(), display, false);
        }
        ItemBehaviorDescription behavior = entry.clientBehavior();
        if (behavior.use().animation() == ItemBehaviorDescription.UseAnimation.NONE
                && behavior.attack().animation() == ItemBehaviorDescription.SwingAnimation.WHACK
                && behavior.interact().animation() == ItemBehaviorDescription.SwingAnimation.WHACK) {
            context.playerAnimations().registerItemUsingJavaSwing(id);
        }
        return GeneratedItemVariant.block(id, rule.conditions(), display, true);
    }

    private GeneratedItemVariant writeIcon(BedrockItemGenerationContext context,
                                           Key id,
                                           ItemBindingRule rule,
                                           List<ItemBindingRule> parts,
                                           List<JavaModel> models) throws IOException {
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
        // 除了背包图标，是否还需要生成手持 attachable
        boolean attachable = !models.isEmpty() && this.renderer.needsHandheldAttachable(models.getFirst());
        if (attachable) {
            BedrockItemAttachables.write(id, models.getFirst(), icon.image(), texture, context.root());
        }
        return GeneratedItemVariant.icon(id, rule.conditions(), texture, attachable);
    }
}
