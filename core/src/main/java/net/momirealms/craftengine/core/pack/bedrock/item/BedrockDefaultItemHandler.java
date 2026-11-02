package net.momirealms.craftengine.core.pack.bedrock.item;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> rules = entry.definition().rules();
        JavaModelResolver.RenderType type = this.models.classify(entry.definition());
        List<GeneratedItemVariant> variants = new ObjectArrayList<>(rules.size());
        for (int i = 0; i < rules.size(); i++) {
            ItemBindingRule rule = rules.get(i);
            Key id = rules.size() == 1 ? entry.bedrockId()
                    : Key.of(entry.bedrockId().namespace(), entry.bedrockId().value() + "_variant_" + i);
            try {
                variants.add(this.writeVariant(entry, context, id, rule, type));
            } catch (UnsupportedOperationException exception) {
                // 注册计划保留这个分支及跳过原因，后面的分支仍可生成。
                variants.add(GeneratedItemVariant.skipped(id, rule, exception.getMessage()));
            }
        }
        return variants;
    }

    private GeneratedItemVariant writeVariant(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context, Key id,
                                               ItemBindingRule rule, JavaModelResolver.RenderType type) throws IOException {
        List<ItemBindingRule> parts;
        if (rule.target() instanceof ItemRenderTarget.Model) {
            parts = List.of(rule);
        } else if (rule.target() instanceof ItemRenderTarget.Composite composite) {
            parts = composite.parts();
        } else {
            return GeneratedItemVariant.skipped(id, rule, null);
        }
        List<JavaModel> models = new ObjectArrayList<>(parts.size());
        boolean separateHandPoses = false;
        for (ItemBindingRule part : parts) {
            if (!(part.target() instanceof ItemRenderTarget.Model target)) {
                throw new UnsupportedOperationException("Unsupported composite child: " + part.sourcePath());
            }
            JavaModel model = this.models.resolve(target.model());
            if (!models.isEmpty() && !model.display().equals(models.getFirst().display())) {
                validateDisplay(models.getFirst(), model, type);
                separateHandPoses = true;
            }
            models.add(model);
        }
        if (type == JavaModelResolver.RenderType.BLOCK) {
            return this.writeDisplayBlock(entry, context, id, rule, parts);
        }
        if (type == JavaModelResolver.RenderType.ITEM) {
            return this.writeIcon(context, id, rule, parts, models, separateHandPoses);
        }
        return GeneratedItemVariant.skipped(id, rule, null);
    }

    private static void validateDisplay(JavaModel first, JavaModel model, JavaModelResolver.RenderType type) {
        for (DisplayPosition view : DisplayPosition.values()) {
            if (first.display().get(view).equals(model.display().get(view))) {
                continue;
            }
            boolean handheld = switch (view) {
                case FIRSTPERSON_RIGHTHAND, FIRSTPERSON_LEFTHAND, THIRDPERSON_RIGHTHAND, THIRDPERSON_LEFTHAND -> true;
                default -> false;
            };
            if (!handheld || type != JavaModelResolver.RenderType.ITEM) {
                throw new UnsupportedOperationException("Composite models have different " + view.name().toLowerCase(Locale.ROOT)
                        + " display transforms: " + first.id() + " and " + model.id());
            }
        }
    }

    private GeneratedItemVariant writeDisplayBlock(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context, Key id,
                                                    ItemBindingRule rule, List<ItemBindingRule> parts) throws IOException {
        BedrockItemBlocks.DisplayBlock display = context.blocks().write(id, parts, this.models, this.textures, context.root());
        if (!BedrockAnimatedGeneratedItemAttachables.write(id, rule, this.models, this.textures, context.root())) {
            return GeneratedItemVariant.block(id, rule, display, false);
        }
        ItemBehaviorDescription behavior = entry.source().clientBehavior();
        if (behavior.use().animation() == ItemBehaviorDescription.UseAnimation.NONE
                && behavior.attack().animation() == ItemBehaviorDescription.SwingAnimation.WHACK
                && behavior.interact().animation() == ItemBehaviorDescription.SwingAnimation.WHACK) {
            context.javaSwings().add(id);
        }
        return GeneratedItemVariant.block(id, rule, display, true);
    }

    private GeneratedItemVariant writeIcon(BedrockItemGenerationContext context, Key id, ItemBindingRule rule, List<ItemBindingRule> parts,
                                           List<JavaModel> models, boolean separateHandPoses) throws IOException {
        List<BufferedImage> images = new ObjectArrayList<>(parts.size());
        for (ItemBindingRule part : parts) {
            BufferedImage image = this.renderer.render((ItemRenderTarget.Model) part.target());
            if (image == null) {
                throw new UnsupportedOperationException("Model cannot be rendered as a static icon: " + part.sourcePath());
            }
            images.add(image);
        }
        BufferedImage icon = ImageUtils.composite(images);
        String texture = "textures/" + id.namespace() + '/' + id.value() + "/icon";
        ImageUtils.writePng(icon, context.root().resolve(texture + ".png"));
        boolean attachable = separateHandPoses || (!models.isEmpty() && this.models.needsHandheldAttachable(models.getFirst()));
        if (separateHandPoses) {
            BedrockItemAttachables.writeComposite(id, models, images, context.root());
        } else if (attachable) {
            BedrockItemAttachables.write(id, models.getFirst(), icon, texture, context.root());
        }
        return GeneratedItemVariant.icon(id, rule, texture, attachable);
    }
}
