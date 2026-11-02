package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

final class BedrockDisplayContextItems implements BedrockItemHandler {
    private static final List<ItemDisplayContext> EQUIPPED = List.of(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.HEAD);
    private static final List<ItemDisplayContext> WORLD = List.of(ItemDisplayContext.GROUND, ItemDisplayContext.FIXED, ItemDisplayContext.ON_SHELF);
    private final JavaModelResources resources;
    private final BedrockItemIconRenderer renderer;

    BedrockDisplayContextItems(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        this.resources = resources;
        this.renderer = renderer;
    }

    @Override
    public List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException {
        if (!DisplayContextVariants.usesDisplayContext(entry)) {
            return null;
        }
        List<DisplayContextVariants.Variant> variants = DisplayContextVariants.collect(entry);
        JsonElement equipment = entry.properties().components().get(DataComponentKeys.EQUIPPABLE);
        boolean headEquipment = equipment != null && equipment.isJsonObject() && "head".equals(equipment.getAsJsonObject().get("slot").getAsString());
        List<GeneratedItemVariant> output = new ArrayList<>();
        for (int i = 0; i < variants.size(); i++) {
            DisplayContextVariants.Variant variant = variants.get(i);
            Key baseId = entry.bedrockId();
            Key id = variants.size() == 1 ? baseId : Key.of(baseId.namespace(), baseId.value() + "_variant_" + i);
            EnumMap<ItemDisplayContext, ItemBindingRule> models = new EnumMap<>(ItemDisplayContext.class);
            for (ItemDisplayContext view : ItemDisplayContext.values()) {
                models.put(view, variant.idle(view));
            }
            ItemBindingRule gui = models.get(ItemDisplayContext.GUI);
            ItemBindingRule registration = new ItemBindingRule(variant.conditions(), gui.target(), gui.transformations(), gui.sourcePath());
            GeneratedItemVariant inventory;
            try {
                inventory = this.writeVisual(id, registration, gui, ItemDisplayContext.GUI, context);
            } catch (UnsupportedOperationException | IOException e) {
                warn(id, ItemDisplayContext.GUI, e);
                inventory = transparent(id, registration, context);
            }
            Key headId = headEquipment ? Key.of(id.namespace(), id.value() + "_view_head") : null;
            this.writeEquipment(id, headId, entry.useBehavior(), variant, context);
            // World aliases have priority over the inventory item only inside the corresponding Geyser translator.
            for (ItemDisplayContext view : WORLD) {
                ItemBindingRule rule = models.get(view);
                if (rule.target().equals(gui.target()) && rule.transformations().equals(gui.transformations())
                        && inventory.displayBlock() == null) {
                    continue;
                }
                Key alias = Key.of(id.namespace(), id.value() + "_view_" + BedrockItemDisplays.viewName(view));
                try {
                    GeneratedItemVariant visual = this.writeVisual(alias, registration, rule, view, context);
                    output.add(new GeneratedItemVariant(alias, registration, visual.iconTexture(), visual.displayBlock(), false, visual.dyeColor(), null, Map.of(), view, null));
                } catch (UnsupportedOperationException | IOException e) {
                    warn(id, view, e);
                }
            }
            output.add(new GeneratedItemVariant(id, registration, inventory.iconTexture(), inventory.displayBlock(), true, inventory.dyeColor(), null, models, null, headId));
        }
        return output;
    }

    private GeneratedItemVariant writeVisual(Key id, ItemBindingRule registration, ItemBindingRule model, ItemDisplayContext view,
                                            BedrockItemGenerationContext context) throws IOException {
        List<ItemBindingRule> parts = parts(model);
        if (parts.isEmpty()) {
            return transparent(id, registration, context);
        }
        List<BufferedImage> images = new ArrayList<>(parts.size());
        for (ItemBindingRule part : parts) {
            if (!part.transformations().isEmpty()) {
                break;
            }
            BufferedImage image = this.renderer.render((ItemRenderTarget.Model) part.target());
            if (image == null) {
                break;
            }
            images.add(image);
        }
        if (images.size() == parts.size()) {
            String path = iconPath(id);
            ImageUtils.writePng(ImageUtils.composite(images), context.root().resolve(path + ".png"));
            return GeneratedItemVariant.icon(id, registration, path, false);
        }
        BedrockItemBlocks.DisplayBlock block = context.itemDisplayBlocks().write(id, parts, this.resources.models(), this.resources.textures(),
                context.root(), DisplayPosition.valueOf(view.name()));
        return GeneratedItemVariant.block(id, registration, block, false);
    }

    private void writeEquipment(Key id, Key headId, ItemBehaviorDescription.Use use, DisplayContextVariants.Variant variant,
                                BedrockItemGenerationContext context) throws IOException {
        BedrockWeaponGeometry mesh = new BedrockWeaponGeometry(this.resources.models(), this.resources.textures());
        Map<Layer, List<String>> layers = new LinkedHashMap<>();
        for (ItemDisplayContext view : EQUIPPED) {
            for (ItemBindingRule rule : variant.rules().get(view)) {
                try {
                    String condition = "(" + equippedView(view).condition + ") && (" + BedrockItemAttachables.useCondition(rule) + ")";
                    for (ItemBindingRule part : parts(rule)) {
                        layers.computeIfAbsent(new Layer(view, part), ignored -> new ArrayList<>()).add(condition);
                    }
                } catch (UnsupportedOperationException e) {
                    warn(id, view, e);
                }
            }
        }
        List<Bedrock3DItemAttachables.Stage> stages = new ArrayList<>();
        List<BedrockItemAttachables.View> views = new ArrayList<>();
        for (Map.Entry<Layer, List<String>> layer : layers.entrySet()) {
            try {
                stages.add(mesh.createStage(layer.getKey().part(), String.join(" || ", layer.getValue())));
                views.add(equippedView(layer.getKey().view()));
            } catch (UnsupportedOperationException | IOException e) {
                warn(id, layer.getKey().view(), e);
            }
        }
        JsonArray scripts = new JsonArray();
        scripts.add("variable.ce_using_item = context.item_slot == 'main_hand' && query.is_using_item && query.main_hand_item_use_duration > 0;");
        scripts.add("variable.ce_use_ticks = variable.ce_using_item ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration) : 0;");
        scripts.add("variable.ce_remaining_ticks = variable.ce_using_item ? math.max(0, " + use.maximumDuration().value() + " - variable.ce_use_ticks) : 0;");
        Bedrock3DItemAttachables.writeDisplayContexts(id, headId, stages, views, mesh.materials(), scripts, context.root());
    }

    private record Layer(ItemDisplayContext view, ItemBindingRule part) {
    }

    private static BedrockItemAttachables.View equippedView(ItemDisplayContext view) {
        return switch (view) {
            case FIRST_PERSON_RIGHT_HAND -> BedrockItemAttachables.View.FIRST_RIGHT;
            case FIRST_PERSON_LEFT_HAND -> BedrockItemAttachables.View.FIRST_LEFT;
            case THIRD_PERSON_RIGHT_HAND -> BedrockItemAttachables.View.THIRD_RIGHT;
            case THIRD_PERSON_LEFT_HAND -> BedrockItemAttachables.View.THIRD_LEFT;
            case HEAD -> BedrockItemAttachables.View.HEAD;
            default -> throw new IllegalArgumentException(view.name());
        };
    }

    private static List<ItemBindingRule> parts(ItemBindingRule rule) {
        if (rule.target() == ItemRenderTarget.Builtin.EMPTY) {
            return List.of();
        }
        List<ItemBindingRule> parts = rule.target() instanceof ItemRenderTarget.Composite composite ? composite.parts() : List.of(rule);
        for (ItemBindingRule part : parts) {
            if (!(part.target() instanceof ItemRenderTarget.Model)) {
                throw new UnsupportedOperationException("Unsupported display model at " + part.sourcePath());
            }
        }
        return parts;
    }

    private static GeneratedItemVariant transparent(Key id, ItemBindingRule registration, BedrockItemGenerationContext context) throws IOException {
        String path = iconPath(id);
        ImageUtils.writePng(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), context.root().resolve(path + ".png"));
        return GeneratedItemVariant.icon(id, registration, path, false);
    }

    private static String iconPath(Key id) {
        return "textures/" + id.namespace() + '/' + id.value() + "/icon";
    }

    private static void warn(Key id, ItemDisplayContext view, Exception e) {
        TranslationManager.warn("resource_pack.bedrock.item.display_skipped", id.asString(), BedrockItemDisplays.viewName(view), e.toString());
    }
}
