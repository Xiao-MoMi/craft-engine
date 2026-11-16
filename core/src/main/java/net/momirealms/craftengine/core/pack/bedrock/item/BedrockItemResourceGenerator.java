package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.render.BedrockItemWriter;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BedrockItemResourceGenerator {
    private BedrockItemResourceGenerator() {
    }

    public static Result write(JavaModelResources resources, ItemRegistrationPlan plan, Path root, BedrockTerrainAtlas atlas, JsonObject blockDefinitions) throws IOException {
        BedrockItemIconRenderer renderer = new BedrockItemIconRenderer(resources);
        BedrockItemWriter itemWriter = new BedrockItemWriter(resources, renderer);
        BedrockItemGenerationContext context = new BedrockItemGenerationContext(root, atlas, blockDefinitions);

        Map<Key, List<GeneratedItemVariant>> items = new LinkedHashMap<>();
        for (ResolvedItemBinding entry : plan.entries()) {
            try {
                items.put(entry.bedrockId(), itemWriter.write(entry, context));
            } catch (IOException | RuntimeException t) {
                throw new IOException("Failed to generate item resources for " + entry.itemModel() + " at " + ResourcePackUtils.itemDefinitionPath(entry.itemModel()) + ": " + t.getMessage(), t);
            }
        }

        writeSharedResources(context, items);
        return new Result(items, context.elytraEquipment().variants());
    }

    private static void writeSharedResources(BedrockItemGenerationContext context, Map<Key, List<GeneratedItemVariant>> items) throws IOException {
        Path root = context.root();
        // 所有变体生成后，将图标及其染色遮罩的纹理路径汇总到 textures/item_texture.json
        writeItemAtlas(root, items);
        context.playerAnimations().write(root);
        context.animalEquipment().write(root);
        context.elytraEquipment().write(root);
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

    public record Result(Map<Key, List<GeneratedItemVariant>> variants, Map<Key, Integer> elytraVariants) {
    }
}
