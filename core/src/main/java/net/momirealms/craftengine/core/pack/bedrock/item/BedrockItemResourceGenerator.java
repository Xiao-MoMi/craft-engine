package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.pack.bedrock.block.BedrockBlockDefinitions;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.render.BedrockItemWriter;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.pack.bedrock.texture.BedrockTerrainAtlas;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourcePackUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.NAMESPACE;

public final class BedrockItemResourceGenerator {
    private BedrockItemResourceGenerator() {
    }

    public static Result write(JavaModelResources resources, ItemRegistrationPlan plan, Path root, BedrockTerrainAtlas atlas, BedrockBlockDefinitions blockDefinitions) throws IOException {
        BedrockItemIconRenderer renderer = new BedrockItemIconRenderer(resources);
        BedrockItemWriter itemWriter = new BedrockItemWriter(resources, renderer);
        BedrockItemGenerationContext context = new BedrockItemGenerationContext(root, atlas, blockDefinitions);

        Map<Key, List<GeneratedItemVariant>> items = LinkedHashMap.newLinkedHashMap(plan.size());
        for (ResolvedItemBinding entry : plan.explicitItems()) {
            try {
                items.put(entry.bedrockId(), itemWriter.write(entry, context));
            } catch (IOException | RuntimeException exception) {
                throw new IOException("Failed to generate item resources for " + entry.itemModel() + " at " + ResourcePackUtils.itemDefinitionPath(entry.itemModel()) + ": " + exception.getMessage(), exception);
            }
        }
        for (ResolvedItemBinding entry : plan.vanillaOverrides()) {
            try {
                items.put(entry.bedrockId(), itemWriter.write(entry, context));
            } catch (IOException | RuntimeException exception) {
                TranslationManager.warn("resource_pack.bedrock.item.variant_skipped", entry.bedrockId().asString(), entry.itemModel().asString(),
                        ResourcePackUtils.itemDefinitionPath(entry.itemModel()), exception.getLocalizedMessage());
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
        shareDyedIconTextures(root, icons);
        JsonObject atlas = new JsonObject();
        atlas.addProperty("resource_pack_name", NAMESPACE);
        atlas.addProperty("texture_name", "atlas.items");
        atlas.add("texture_data", icons);
        Path output = root.resolve(BedrockPackLayout.ITEM_TEXTURE_ATLAS);
        Files.createDirectories(output.getParent());
        GsonHelper.writeJsonFile(GsonHelper.sortObjectKeys(atlas), output);
    }

    private static void shareDyedIconTextures(Path root, JsonObject icons) throws IOException {
        Map<String, String> pathsByHash = new HashMap<>();
        Map<String, String> resolvedPaths = new HashMap<>();
        byte[] buffer = new byte[8192];
        for (String key : icons.keySet()) {
            if (!key.startsWith(BedrockPackLayout.DYED_ICON_PREFIX)) {
                continue;
            }
            JsonObject icon = icons.getAsJsonObject(key);
            String texture = icon.get("textures").getAsString();
            String shared = resolvedPaths.get(texture);
            if (shared == null) {
                Path file = root.resolve(texture);
                String existing = pathsByHash.putIfAbsent(HashUtils.sha1(file, buffer), texture);
                shared = existing != null && Files.mismatch(file, root.resolve(existing)) == -1 ? existing : texture;
                resolvedPaths.put(texture, shared);
            }
            if (!texture.equals(shared)) {
                icon.addProperty("textures", shared);
            }
        }
        // 场景别名可能指向同一个文件，等所有图标键都已重定向后再移除重复纹理。
        for (Map.Entry<String, String> entry : resolvedPaths.entrySet()) {
            if (!entry.getKey().equals(entry.getValue())) {
                Files.delete(root.resolve(entry.getKey()));
            }
        }
    }

    public record Result(Map<Key, List<GeneratedItemVariant>> variants, Map<Key, Integer> elytraVariants) {
    }
}
