package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemResourceGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class ItemRegistrationPlanWriter {

    private ItemRegistrationPlanWriter() {
    }

    public static void write(ItemRegistrationPlan plan, Path itemPlanPath, BedrockItemResourceGenerator.Result generatedItems) throws IOException {
        write(plan, itemPlanPath, generatedItems, new JsonObject());
    }

    public static void write(ItemRegistrationPlan plan, Path itemPlanPath, BedrockItemResourceGenerator.Result generatedItems, JsonObject itemDisplays) throws IOException {
        JsonObject json = toJson(plan, generatedItems);
        if (!itemDisplays.isEmpty()) {
            json.add("item_displays", itemDisplays);
        }
        Files.writeString(itemPlanPath, json.toString());
    }

    public static JsonObject toJson(ItemRegistrationPlan plan, BedrockItemResourceGenerator.Result generatedItems) {
        JsonObject result = new JsonObject();
        result.addProperty(KIND, RegistrationPlanKind.ITEM.id());
        result.addProperty(FORMAT_VERSION, RegistrationPlanKind.ITEM.formatVersion());
        result.addProperty(SOURCE_VERSION, VersionHelper.MINECRAFT_VERSION.version());

        JsonArray entries = new JsonArray(plan.entries().size());
        for (ItemRegistrationPlan.Entry entry : plan.entries()) {
            List<GeneratedItemVariant> variants = generatedItems.variants().getOrDefault(entry.bedrockId(), List.of());
            entries.add(entryToJson(entry, variants));
        }
        result.add(ENTRIES, entries);
        return GsonHelper.sortObjectKeys(result).getAsJsonObject();
    }

    private static JsonObject entryToJson(ItemRegistrationPlan.Entry entry, List<GeneratedItemVariant> variants) {
        ResolvedItemBinding binding = entry.source();
        JsonObject json = new JsonObject();
        json.addProperty(BEDROCK_IDENTIFIER, entry.bedrockId().asString());
        json.addProperty(ITEM_TYPE, entry.javaItemType().asString());
        json.addProperty(ITEM_ID, binding.itemId().asString());
        json.addProperty(MODEL, entry.definitionId().asString());

        if (!entry.properties().components().isEmpty()) {
            JsonObject components = new JsonObject();
            // 显式的 JSON null 表示移除原版组件。
            entry.properties().components().forEach((key, value) -> components.add(key.asString(), value));
            json.add(COMPONENTS, components);
        }

        // Geyser 按数组顺序分配优先级，靠前的变体优先。
        JsonArray branches = new JsonArray(variants.size());
        for (GeneratedItemVariant variant : variants) {
            branches.add(variantToJson(variant));
        }
        json.add(VARIANTS, branches);

        JsonObject behavior = new JsonObject();
        behavior.add("use", GsonHelper.get().toJsonTree(entry.useBehavior()));
        if (binding.placesBlock()) {
            behavior.addProperty(PLACES_BLOCK, true);
        }
        json.add(CLIENT_BEHAVIOR, behavior);
        return json;
    }

    private static JsonObject variantToJson(GeneratedItemVariant variant) {
        JsonObject json = new JsonObject();
        json.addProperty(BEDROCK_IDENTIFIER, variant.id().asString());
        if (variant.headId() != null) {
            json.addProperty(HEAD_IDENTIFIER, variant.headId().asString());
        }
        if (variant.worldView() != null) {
            json.addProperty("world_view", variant.worldView().name());
        }
        List<ItemBindingCondition> conditions = variant.rule().conditions();
        JsonArray matches = new JsonArray(conditions.size());
        for (ItemBindingCondition condition : conditions) {
            matches.add(conditionToJson(condition));
        }
        json.add(CONDITIONS, matches);
        json.add(RESOURCES, resourcesToJson(variant));
        return json;
    }

    private static JsonObject conditionToJson(ItemBindingCondition condition) {
        JsonObject json = new JsonObject();
        switch (condition) {
            case ItemBindingCondition.BooleanMatch match -> {
                match.property().writeProperty(json);
                json.addProperty(EXPECTED, match.expected());
            }
            case ItemBindingCondition.SelectMatch select -> {
                select.property().writeProperty(json);
                JsonArray values = new JsonArray(select.values().size());
                select.values().forEach(values::add);
                json.add(VALUES, values);
                json.addProperty(NEGATED, select.negated());
            }
            case ItemBindingCondition.NumericRange range -> {
                range.property().writeProperty(json);
                json.addProperty(SCALE, range.scale());
                if (range.minInclusive() != null) {
                    json.addProperty(MIN, range.minInclusive());
                }
                if (range.maxExclusive() != null) {
                    json.addProperty(MAX, range.maxExclusive());
                }
            }
        }
        return json;
    }

    private static JsonObject resourcesToJson(GeneratedItemVariant variant) {
        JsonObject resources = new JsonObject();
        if (variant.displayBlock() != null) {
            resources.addProperty(DISPLAY_BLOCK, variant.displayBlock().name());
        }
        if (variant.dyeColor() != null) {
            resources.addProperty(DYE_COLOR, variant.dyeColor());
        }
        resources.addProperty(ATTACHABLE, variant.attachable());
        resources.addProperty(GENERATED, variant.generated());
        if (variant.unsupportedReason() != null) {
            resources.addProperty(UNSUPPORTED_REASON, variant.unsupportedReason());
        }
        return resources;
    }
}
