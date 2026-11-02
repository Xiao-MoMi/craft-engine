package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.JavaPackTextureSource;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockItemResourceGenerator {
    private BedrockItemResourceGenerator() {
    }

    public static Result write(JavaModelResources resources, ItemRegistrationPlan plan, Path root) throws IOException {
        BedrockItemIconRenderer renderer = new BedrockItemIconRenderer(resources);
        BedrockDefaultItemHandler defaultHandler = new BedrockDefaultItemHandler(resources, renderer);
        Map<Key, List<BedrockItemHandler>> handlers = createHandlers(resources, renderer);
        BedrockItemGenerationContext context = new BedrockItemGenerationContext(root);

        Map<Key, List<GeneratedItemVariant>> items = new LinkedHashMap<>();
        for (ItemRegistrationPlan.Entry entry : plan.entries()) {
            items.put(entry.bedrockId(), writeItem(entry, context, handlers.getOrDefault(entry.javaItemType(), List.of()), defaultHandler));
        }
        writeSharedResources(context, items);
        return new Result(items);
    }

    private static List<GeneratedItemVariant> writeItem(ItemRegistrationPlan.Entry entry,
                                                        BedrockItemGenerationContext context,
                                                        List<BedrockItemHandler> handlers,
                                                        BedrockDefaultItemHandler defaultHandler) throws IOException {
        try {
            for (BedrockItemHandler handler : handlers) {
                List<GeneratedItemVariant> variants = handler.write(entry, context);
                if (variants != null) {
                    return variants;
                }
            }
            return defaultHandler.write(entry, context);
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Failed to generate item resources for " + entry.definitionId() + " at " + ResourcePackUtils.itemDefinitionPath(entry.definitionId()) + ": " + exception.getMessage(), exception);
        }
    }

    private static void writeSharedResources(BedrockItemGenerationContext context, Map<Key, List<GeneratedItemVariant>> items) throws IOException {
        Path root = context.root();
        writeItemAtlas(root, items);
        context.blocks().finish(root);
        context.crossbowPlayerAnimations().write(root);
        BedrockSpearPlayerAnimations.write(context.javaSpears(), root);
        BedrockShieldPlayerAnimations.write(context.javaShields(), root);
        BedrockBowPlayerAnimations.write(context.bows(), context.javaBows(), root);
        BedrockSwingPlayerAnimations.write(context.javaSwings(), root);
    }

    private static void writeItemAtlas(Path root, Map<Key, List<GeneratedItemVariant>> items) throws IOException {
        JsonObject icons = new JsonObject();
        for (List<GeneratedItemVariant> variants : items.values()) {
            for (GeneratedItemVariant variant : variants) {
                if (variant.iconTexture() != null) {
                    JsonObject icon = new JsonObject();
                    icon.addProperty("textures", variant.iconTexture());
                    icons.add(variant.id().asString(), icon);
                    if (variant.dyeColor() != null) {
                        JsonObject mask = new JsonObject();
                        mask.addProperty("textures", variant.iconTexture() + "_dyed.tga");
                        icons.add(BedrockPackLayout.dyedIconKey(variant.id().asString()), mask);
                    }
                }
            }
        }
        JsonObject atlas = new JsonObject();
        atlas.addProperty("resource_pack_name", "craftengine");
        atlas.addProperty("texture_name", "atlas.items");
        atlas.add("texture_data", icons);
        Path output = root.resolve(BedrockPackLayout.ITEM_TEXTURE_ATLAS);
        Files.createDirectories(output.getParent());
        GsonHelper.writeJsonFile(GsonHelper.sortObjectKeys(atlas), output);
    }

    private static Map<Key, List<BedrockItemHandler>> createHandlers(JavaModelResources resources, BedrockItemIconRenderer renderer) {
        JavaModelResolver models = resources.models();
        JavaPackTextureSource textures = resources.textures();
        Map<Key, List<BedrockItemHandler>> handlers = new HashMap<>();
        handlers.put(ItemKeys.BOW, List.of(new Bedrock2DBowItemHandler(models, renderer), new Bedrock3DBowItemHandler(models, textures)));
        handlers.put(ItemKeys.CROSSBOW, List.of(new Bedrock2DCrossbowItemHandler(models, renderer), new Bedrock3DCrossbowItemHandler(models, textures)));
        handlers.put(ItemKeys.SHIELD, List.of(new Bedrock3DShieldItemHandler(resources)));
        List<BedrockItemHandler> spears = List.of(new Bedrock2DSpearItemHandler(renderer), new Bedrock3DSpearItemHandler(models, textures));
        for (Key spear : ItemKeys.SPEARS) {
            handlers.put(spear, spears);
        }
        return handlers;
    }

    public record Result(Map<Key, List<GeneratedItemVariant>> variants) {
    }
}
