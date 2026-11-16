package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.BedrockResourceException;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockDyedItem;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemBlocks;
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
    private final BedrockDisplayContextItems displayContextItems;

    public BedrockItemWriter(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        this.resources = resources;
        this.renderer = renderer;
        this.displayContextItems = new BedrockDisplayContextItems(resources, renderer);
    }

    public List<GeneratedItemVariant> write(ResolvedItemBinding entry, BedrockItemGenerationContext context) throws IOException {
        // 是个特殊的模型，比如弓
        List<BedrockItemPresentation> presentations = BedrockItemPresentation.plan(entry, this.resources.models());
        if (presentations != null) {
            return this.displayContextItems.write(entry, presentations, context);
        }

        // 按照标准流程生成
        List<ItemBindingRule> rules = entry.definition().rules();
        // 按照方块还是物品渲染
        ItemRenderType type = ItemRenderType.classify(entry.definition(), this.renderer);
        List<GeneratedItemVariant> variants = new ObjectArrayList<>(rules.size());
        Key bedrockId = entry.bedrockId();
        for (int i = 0, size = rules.size(); i < size; i++) {
            ItemBindingRule rule = rules.get(i);
            Key id = size == 1 ? bedrockId : bedrockId.withSameNamespace(bedrockId.value() + "_variant_" + i);
            try {
                GeneratedItemVariant variant = this.writeVariant(entry, context, id, rule, type);
                variant = variant.withDisplayModels(BedrockItemPresentation.ordinaryDisplayModels(entry, variant.conditions()));
                this.displayContextItems.writeWorldAliases(variant, false, context, variants);
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
                                              ItemRenderType type) throws IOException {
        List<ItemBindingRule> parts;
        if (rule.target() instanceof ItemRenderTarget.Model) {
            parts = List.of(rule);
        } else if (rule.target() instanceof ItemRenderTarget.Composite composite) {
            parts = composite.parts();
        } else {
            throw new BedrockResourceException("resource_pack.bedrock.reason.render_target", rule.target().toString());
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
            case BLOCK -> this.writeDisplayBlock(entry, context, id, rule, parts);
            case ITEM -> this.writeIcon(context, id, rule, parts, models);
        };
    }

    private GeneratedItemVariant writeDisplayBlock(ResolvedItemBinding entry,
                                                   BedrockItemGenerationContext context,
                                                   Key id,
                                                   ItemBindingRule rule,
                                                   List<ItemBindingRule> parts) throws IOException {
        if (rule.target() instanceof ItemRenderTarget.Model target && !target.tints().isEmpty() && rule.transformations().isEmpty()) {
            GeneratedItemVariant dyed = BedrockDyedItem.write(entry, id, rule, target, this.resources.models(), this.resources.textures(), context);
            if (dyed != null) {
                return dyed;
            }
        }
        BedrockItemBlocks.DisplayBlock display = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(), context.root());
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
