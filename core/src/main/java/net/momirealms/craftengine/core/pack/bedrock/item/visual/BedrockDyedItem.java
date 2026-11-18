package net.momirealms.craftengine.core.pack.bedrock.item.visual;

import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.render.BedrockItemGeometryBuilder;
import net.momirealms.craftengine.core.pack.bedrock.model.BedrockItemGeometry;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.pack.bedrock.texture.TextureUsage;
import net.momirealms.craftengine.core.pack.model.definition.tint.CustomModelDataTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.SimpleDefaultTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.MAIN_HAND_DYE;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.OFF_HAND_DYE;

public final class BedrockDyedItem {
    private static final Key DYE = Key.minecraft("dye");

    private BedrockDyedItem() {
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    public static boolean hasDye(List<ItemBindingRule> rules) {
        for (ItemBindingRule rule : rules) {
            if (rule.target() instanceof ItemRenderTarget.Model model) {
                for (Tint tint : model.tints()) {
                    if (tint instanceof CustomModelDataTint || (tint instanceof SimpleDefaultTint source && source.type().equals(DYE))) {
                        return true;
                    }
                }
            } else if (rule.target() instanceof ItemRenderTarget.Composite composite && hasDye(composite.parts())) {
                return true;
            }
        }
        return false;
    }

    public static GeneratedItemVariant writeIcon(Key id,
                                                 List<ItemBindingCondition> conditions,
                                                 List<ItemBindingRule> parts,
                                                 JavaModelResources resources,
                                                 BedrockItemGenerationContext context,
                                                 DisplayPosition view,
                                                 int defaultColor) throws IOException {
        BedrockItemGeometryBuilder mesh = new BedrockItemGeometryBuilder(resources.models(), resources.textures(), TextureUsage.ITEM, true);
        BedrockDyedItemIcon icon = new BedrockDyedItemIcon(Config.bedrockItemIconSize());
        for (ItemBindingRule part : parts) {
            BedrockItemGeometry geometry = mesh.build(part, true);
            icon.render(geometry, mesh.materials(), view);
        }
        String texture = "textures/" + id.namespace() + '/' + id.value() + "/icon";
        icon.write(context.root(), texture);
        return new GeneratedItemVariant(id, conditions, texture, null, false, defaultColor);
    }

    public static String colorScript() {
        return "variable.ce_hand_dye = context.item_slot == 'off_hand' ? (" + viewColor(OFF_HAND_DYE) + ") : (context.item_slot == 'main_hand' ? (" + viewColor(MAIN_HAND_DYE) + ") : -1);";
    }

    private static String viewColor(String property) {
        return "context.is_first_person ? (" + handColor(property) + ") : (" + handColor(property + "_third") + ")";
    }

    private static String handColor(String property) {
        String owner = "context.owning_entity->query.";
        String high = owner + "property('" + property + "_r')";
        return owner + "has_property('" + property + "_r') && " + high + " >= 0 ? " + high + " * 65536 + " + owner + "property('" + property + "_gb') : -1";
    }
}
