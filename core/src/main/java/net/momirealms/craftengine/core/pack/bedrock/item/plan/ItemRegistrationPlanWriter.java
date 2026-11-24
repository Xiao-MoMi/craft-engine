package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemResourceGenerator;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class ItemRegistrationPlanWriter {

    private ItemRegistrationPlanWriter() {
    }

    public static void write(ItemRegistrationPlan plan, Path itemPlanPath, BedrockItemResourceGenerator.Result generatedItems) throws IOException {
        GsonHelper.writeJsonFileWithNulls(toJson(plan, generatedItems), itemPlanPath);
    }

    public static JsonObject toJson(ItemRegistrationPlan plan, BedrockItemResourceGenerator.Result generatedItems) {
        JsonObject result = new JsonObject();
        result.addProperty(KIND, RegistrationPlanKind.ITEM.id());
        result.addProperty(FORMAT_VERSION, RegistrationPlanKind.ITEM.formatVersion());
        result.addProperty(SOURCE_VERSION, VersionHelper.MINECRAFT_VERSION.version());

        JsonArray entries = new JsonArray(plan.size());
        appendEntries(entries, plan.explicitItems(), generatedItems, false);
        appendEntries(entries, plan.vanillaOverrides(), generatedItems, true);
        result.add(ENTRIES, entries);
        return GsonHelper.sortObjectKeys(result).getAsJsonObject();
    }

    private static void appendEntries(JsonArray entries, List<ResolvedItemBinding> items, BedrockItemResourceGenerator.Result generatedItems, boolean vanillaModelOverride) {
        for (ResolvedItemBinding entry : items) {
            List<GeneratedItemVariant> variants = generatedItems.variants().getOrDefault(entry.bedrockId(), List.of());
            JsonObject json = entryToJson(entry, variants);
            if (vanillaModelOverride) {
                json.addProperty(VANILLA_MODEL_OVERRIDE, true);
            }
            entries.add(json);
        }
    }

    private static JsonObject entryToJson(ResolvedItemBinding entry, List<GeneratedItemVariant> variants) {
        JsonObject json = new JsonObject();
        json.addProperty(BEDROCK_IDENTIFIER, entry.bedrockId().asString());
        json.addProperty(ITEM_TYPE, entry.clientItemType().asString());
        json.addProperty(MODEL, entry.itemModel().asString());

        if (!entry.clientProperties().components().isEmpty()) {
            JsonObject components = new JsonObject();
            // 显式的 JSON null 表示移除原版组件。
            entry.clientProperties().components().forEach((key, value) -> components.add(key.asString(), value));
            json.add(COMPONENTS, components);
        }

        // Geyser 按数组顺序分配优先级，靠前的变体优先。
        JsonArray branches = new JsonArray(variants.size());
        for (GeneratedItemVariant variant : variants) {
            branches.add(variantToJson(variant));
        }
        json.add(VARIANTS, branches);

        ItemBehaviorDescription.Use use = entry.useBehavior();
        JsonObject useData = new JsonObject();
        useData.addProperty(ANIMATION, use.animation().name());
        useData.addProperty(MAX_DURATION_TICKS, use.maximumDuration().value());
        if (use.chargeDuration() != null) {
            useData.addProperty(CHARGE_DURATION_TICKS, use.chargeDuration().value());
        }
        useData.addProperty(NATIVE_USE_ON_RELEASE, use.nativeUseOnRelease());

        JsonObject behavior = new JsonObject();
        behavior.add(USE, useData);
        if (entry.clientBehavior().placesBlock()) {
            behavior.addProperty(PLACES_BLOCK, true);
        }
        if (entry.trimmableArmor()) {
            behavior.addProperty(TRIMMABLE_ARMOR, true);
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
            json.addProperty(WORLD_VIEW, variant.worldView().name());
        }
        if (!variant.inventoryVisible()) {
            json.addProperty(INVENTORY_VISIBLE, false);
        }
        List<ItemBindingCondition> conditions = variant.conditions();
        if (!conditions.isEmpty()) {
            JsonArray matches = new JsonArray(conditions.size());
            for (ItemBindingCondition condition : conditions) {
                matches.add(conditionToJson(condition));
            }
            json.add(CONDITIONS, matches);
        }
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
                if (select.negated()) {
                    json.addProperty(NEGATED, true);
                }
            }
            case ItemBindingCondition.NumericRange range -> {
                range.property().writeProperty(json);
                if (range.scale() != 1.0f) {
                    json.addProperty(SCALE, range.scale());
                }
                if (range.minInclusive() != null) {
                    json.addProperty(MIN_INCLUSIVE, range.minInclusive());
                }
                if (range.maxExclusive() != null) {
                    json.addProperty(MAX_EXCLUSIVE, range.maxExclusive());
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
        if (!variant.tintSources().isEmpty()) {
            JsonObject sources = new JsonObject();
            variant.tintSources().forEach((view, source) -> sources.addProperty(view.serializedName(), source));
            resources.add(TINT_SOURCES, sources);
        }
        resources.addProperty(GENERATED, variant.generated());
        return resources;
    }
}
