package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.util.GsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

final class BedrockAtlasObfuscator {
    private BedrockAtlasObfuscator() {
    }

    static void obfuscate(Path pack, Path data) throws IOException {
        obfuscateItems(pack.resolve(ITEM_TEXTURE_ATLAS), data.resolve(RegistrationPlanKind.ITEM.file()));
        obfuscateTerrain(pack.resolve(TERRAIN_TEXTURE_ATLAS), pack.resolve(FLIPBOOK_TEXTURES), data.resolve(RegistrationPlanKind.BLOCK.file()));
    }

    private static void obfuscateItems(Path atlasFile, Path planFile) throws IOException {
        if (!Files.exists(atlasFile) || !Files.exists(planFile)) {
            return;
        }
        JsonObject atlas = GsonHelper.readJsonFromFile(atlasFile).getAsJsonObject();
        JsonObject textures = atlas.getAsJsonObject("texture_data");
        Map<String, String> aliases = BedrockAssetObfuscator.shortNames(textures.keySet(), ID_PREFIX, ID_PREFIX, DYED_ICON_PREFIX);
        if (aliases.isEmpty()) {
            return;
        }
        JsonObject plan = GsonHelper.readJsonFromFile(planFile).getAsJsonObject();
        // 图标原先用物品 ID 查找。短名只写进资源字段，物品注册仍按原 ID 匹配
        for (JsonElement entry : plan.getAsJsonArray(ENTRIES)) {
            for (JsonElement branch : entry.getAsJsonObject().getAsJsonArray(VARIANTS)) {
                JsonObject variant = branch.getAsJsonObject();
                JsonObject resources = variant.getAsJsonObject(RESOURCES);
                String identifier = variant.get(BEDROCK_IDENTIFIER).getAsString();
                String icon = aliases.get(identifier);
                if (icon != null) {
                    resources.addProperty(ICON, icon);
                }
                String dyed = aliases.get(dyedIconKey(identifier));
                if (dyed != null) {
                    resources.addProperty(DYED_ICON, dyed);
                }
            }
        }
        GsonHelper.replaceStringKeys(textures, aliases);
        GsonHelper.writeJsonFile(atlas, atlasFile);
        // 注册计划中的显式 null 用于删除原版组件，序列化时必须保留
        GsonHelper.writeJsonFileWithNulls(plan, planFile);
    }

    private static void obfuscateTerrain(Path atlasFile, Path flipbooksFile, Path planFile) throws IOException {
        if (!Files.exists(atlasFile)) {
            return;
        }
        JsonObject atlas = GsonHelper.readJsonFromFile(atlasFile).getAsJsonObject();
        JsonObject textures = atlas.getAsJsonObject("texture_data");
        Map<String, String> aliases = BedrockAssetObfuscator.shortNames(textures.keySet(), PATH_PREFIX, PATH_PREFIX);
        if (!GsonHelper.replaceStringKeys(textures, aliases)) {
            return;
        }
        GsonHelper.writeJsonFile(atlas, atlasFile);
        if (Files.exists(flipbooksFile)) {
            JsonElement flipbooks = GsonHelper.readJsonFromFile(flipbooksFile);
            boolean changed = false;
            for (JsonElement flipbook : flipbooks.getAsJsonArray()) {
                changed |= GsonHelper.replaceStringValues(flipbook.getAsJsonObject(), "atlas_tile", aliases);
            }
            if (changed) {
                GsonHelper.writeJsonFile(flipbooks, flipbooksFile);
            }
        }
        if (Files.exists(planFile)) {
            JsonObject plan = GsonHelper.readJsonFromFile(planFile).getAsJsonObject();
            boolean changed = false;
            for (String section : List.of(ENTRIES, VANILLA_OVERRIDES)) {
                JsonArray entries = plan.getAsJsonArray(section);
                if (entries == null) {
                    continue;
                }
                for (JsonElement entry : entries) {
                    changed |= GsonHelper.replaceStringValues(entry.getAsJsonObject(), MATERIALS, aliases);
                }
            }
            if (changed) {
                GsonHelper.writeJsonFile(plan, planFile);
            }
        }
    }
}
