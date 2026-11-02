package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.MAIN_HAND_DYE;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.OFF_HAND_DYE;

final class BedrockDyedItem {
    private BedrockDyedItem() {
    }

    @Nullable
    static GeneratedItemVariant write(ItemRegistrationPlan.Entry entry, Key id, ItemBindingRule rule, ItemRenderTarget.Model target,
                                      JavaModelResolver models, JavaPackTextureSource textures, BedrockItemGenerationContext context) throws IOException {
        if (target.tints().stream().noneMatch(tint -> textures.dyeColor(tint) != null)) {
            return null;
        }
        JavaModel model = target.resolve(models);
        if (model.kind() != JavaModel.GeometryKind.ELEMENTS) {
            return null;
        }
        BedrockWeaponGeometry geometry = new BedrockWeaponGeometry(models, textures);
        JsonArray cubes = geometry.dyedGeometry(model, target);
        Integer defaultColor = null;
        for (Bedrock3DItemAttachables.Material material : geometry.materials()) {
            if (material.data().animation() != null) {
                return null;
            }
            if (material.dyeColor() != null) {
                if (defaultColor != null && !defaultColor.equals(material.dyeColor())) {
                    return null;
                }
                defaultColor = material.dyeColor();
            }
        }
        if (defaultColor == null) {
            return null;
        }
        String texture = "textures/" + id.namespace() + '/' + id.value() + "/icon";
        BedrockDyedItemIcon icon = new BedrockDyedItemIcon();
        icon.render(model, target, textures);
        icon.write(context.root(), texture);

        JsonArray scripts = new JsonArray();
        scripts.add("variable.ce_stage = 0;");
        scripts.add("variable.ce_hand_dye = context.item_slot == 'off_hand' ? (" + handColor(OFF_HAND_DYE) + ") : (" + handColor(MAIN_HAND_DYE) + ");");
        Bedrock3DItemAttachables.Stage stage = new Bedrock3DItemAttachables.Stage(model, cubes, "1");
        Bedrock3DItemAttachables.write(id, List.of(stage), geometry.materials(), scripts, new JsonArray(), new JsonObject(), new JsonArray(), context.root());
        BedrockItemBlocks.DisplayBlock block = entry.source().placesBlock()
                ? context.itemDisplayBlocks().write(id, List.of(rule), models, textures, context.root()) : null;
        return new GeneratedItemVariant(id, rule, texture, block, true, defaultColor, null);
    }

    private static String handColor(String property) {
        String owner = "context.owning_entity->query.";
        String high = owner + "property('" + property + "_r')";
        return owner + "has_property('" + property + "_r') && " + high + " >= 0 ? " + high + " * 65536 + " + owner + "property('" + property + "_gb') : -1";
    }
}
