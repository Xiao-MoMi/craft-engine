package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

final class BedrockIdentityObfuscator {
    private static final String QUOTED_ID_PREFIX = "'" + ID_PREFIX;
    private static final String QUOTED_GEYSER_BLOCK_PREFIX = "'" + GEYSER_CUSTOM_BLOCK_PREFIX;
    private static final Pattern MOLANG_IDENTIFIER = Pattern.compile("'((?:" + ID_PREFIX + "|" + GEYSER_CUSTOM_BLOCK_PREFIX + ")[a-z0-9_./-]+)'");

    private BedrockIdentityObfuscator() {
    }

    static Map<String, String> rewritePlans(Path data) throws IOException {
        SecureRandom random = new SecureRandom();
        Map<String, String> blockNames = rewriteBlockPlan(data.resolve(RegistrationPlanKind.BLOCK.file()), random);
        Map<String, String> clientIds = rewriteItemPlan(data.resolve(RegistrationPlanKind.ITEM.file()), blockNames, random);
        blockNames.forEach((original, alias) -> clientIds.put(GEYSER_CUSTOM_BLOCK_PREFIX + original, GEYSER_CUSTOM_BLOCK_PREFIX + alias));
        return clientIds;
    }

    private static Map<String, String> rewriteBlockPlan(Path file, SecureRandom random) throws IOException {
        if (!Files.exists(file)) {
            return Map.of();
        }
        JsonObject plan = GsonHelper.readJsonFromFile(file).getAsJsonObject();
        JsonArray entries = plan.getAsJsonArray(ENTRIES);
        JsonArray overrides = plan.getAsJsonArray(VANILLA_OVERRIDES);
        List<JsonArray> sections = overrides == null ? List.of(entries) : List.of(entries, overrides);
        Set<String> names = new HashSet<>();
        for (JsonArray section : sections) {
            for (JsonElement entry : section) {
                collect(entry.getAsJsonObject(), NAME, PATH_PREFIX, names);
            }
        }
        Map<String, String> aliases = randomNames(names, PATH_PREFIX + "b", random);
        if (!aliases.isEmpty()) {
            for (JsonArray section : sections) {
                for (JsonElement entry : section) {
                    GsonHelper.replaceStringValues(entry.getAsJsonObject(), NAME, aliases);
                }
            }
            GsonHelper.writeJsonFile(plan, file);
        }
        return aliases;
    }

    private static Map<String, String> rewriteItemPlan(Path file, Map<String, String> blockNames, SecureRandom random) throws IOException {
        if (!Files.exists(file)) {
            return new HashMap<>();
        }
        JsonObject plan = GsonHelper.readJsonFromFile(file).getAsJsonObject();
        Set<String> identifiers = new HashSet<>();
        for (JsonElement element : plan.getAsJsonArray(ENTRIES)) {
            JsonObject entry = element.getAsJsonObject();
            collect(entry, BEDROCK_IDENTIFIER, ID_PREFIX, identifiers);
            for (JsonElement branch : entry.getAsJsonArray(VARIANTS)) {
                JsonObject variant = branch.getAsJsonObject();
                collect(variant, BEDROCK_IDENTIFIER, ID_PREFIX, identifiers);
                collect(variant, HEAD_IDENTIFIER, ID_PREFIX, identifiers);
            }
        }
        Map<String, String> aliases = randomNames(identifiers, ID_PREFIX + "i", random);
        if (!aliases.isEmpty() || !blockNames.isEmpty()) {
            // 只更新客户端标识符，Java 物品模型和匹配条件仍供 Geyser 查找原始物品
            for (JsonElement element : plan.getAsJsonArray(ENTRIES)) {
                JsonObject entry = element.getAsJsonObject();
                GsonHelper.replaceStringValues(entry, BEDROCK_IDENTIFIER, aliases);
                for (JsonElement branch : entry.getAsJsonArray(VARIANTS)) {
                    JsonObject variant = branch.getAsJsonObject();
                    GsonHelper.replaceStringValues(variant, BEDROCK_IDENTIFIER, aliases);
                    GsonHelper.replaceStringValues(variant, HEAD_IDENTIFIER, aliases);
                    GsonHelper.replaceStringValues(variant.getAsJsonObject(RESOURCES), DISPLAY_BLOCK, blockNames);
                }
            }
            JsonObject elytra = plan.getAsJsonObject(ELYTRA_VARIANTS);
            if (elytra != null) {
                GsonHelper.replaceStringKeys(elytra, aliases);
            }
            JsonObject displays = plan.getAsJsonObject("item_displays");
            if (displays != null) {
                GsonHelper.replaceStringKeys(displays.getAsJsonObject("items"), aliases);
            }
            GsonHelper.writeJsonFileWithNulls(plan, file);
        }
        return aliases;
    }

    private static void collect(JsonObject object, String key, String namespace, Set<String> names) {
        JsonElement value = object.get(key);
        if (value != null) {
            String identifier = value.getAsString();
            if (identifier.startsWith(namespace)) {
                names.add(identifier);
            }
        }
    }

    private static Map<String, String> randomNames(Set<String> originals, String prefix, SecureRandom random) {
        Map<String, String> aliases = HashMap.newHashMap(originals.size());
        Set<String> used = new HashSet<>(originals);
        long bound = Math.max(36L * 36 * 36 * 36, 8L * originals.size());
        for (String original : originals) {
            String name;
            do {
                name = prefix + Long.toString(random.nextLong(bound), 36);
            } while (!used.add(name));
            aliases.put(original, name);
        }
        return aliases;
    }

    @Nullable
    static String rewriteMolang(String value, Map<String, String> aliases) {
        if (aliases.isEmpty() || (!value.contains(QUOTED_ID_PREFIX) && !value.contains(QUOTED_GEYSER_BLOCK_PREFIX))) {
            return null;
        }
        Matcher matcher = MOLANG_IDENTIFIER.matcher(value);
        StringBuilder result = null;
        while (matcher.find()) {
            String alias = aliases.get(matcher.group(1));
            if (alias == null) {
                continue;
            }
            if (result == null) {
                result = new StringBuilder(value.length());
            }
            matcher.appendReplacement(result, "'" + alias + "'");
        }
        return result == null ? null : matcher.appendTail(result).toString();
    }
}
