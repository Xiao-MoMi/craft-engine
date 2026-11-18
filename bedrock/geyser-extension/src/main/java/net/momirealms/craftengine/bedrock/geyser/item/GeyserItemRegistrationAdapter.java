package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatch;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatches;
import net.momirealms.craftengine.bedrock.geyser.item.predicate.GeyserItemPredicates;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManager;
import net.momirealms.craftengine.bedrock.geyser.proxy.item.custom.GeyserCustomItemDefinitionBuilderProxy;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.CustomItemIcons;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.*;

public final class GeyserItemRegistrationAdapter {
    private final int sourceVersion;

    public GeyserItemRegistrationAdapter(String sourceVersion) {
        String[] parts = sourceVersion.split("\\.");
        this.sourceVersion = Integer.parseInt(parts[0]) * 10000 + Integer.parseInt(parts[1]) * 100 + (parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
        if (this.sourceVersion < 12104) {
            throw new IllegalArgumentException("Legacy model mapping is not implemented");
        }
    }

    public ConvertedItem convert(JsonObject entry, Map<String, String> displayBlocks) {
        Identifier itemType = Identifier.of(entry.get(ITEM_TYPE).getAsString());
        Identifier model = Identifier.of(entry.get(MODEL).getAsString());
        JsonObject behavior = entry.getAsJsonObject(CLIENT_BEHAVIOR);
        boolean placesBlock = behavior != null && behavior.has(PLACES_BLOCK) && behavior.get(PLACES_BLOCK).getAsBoolean();
        JsonArray variants = entry.getAsJsonArray(VARIANTS);
        List<ConvertedVariant> converted = new ArrayList<>(variants.size());
        boolean contextual = false;
        int priority = variants.size();
        int skipped = 0;
        int failed = 0;
        for (JsonElement branch : variants) {
            int branchPriority = priority--;
            if (!branch.isJsonObject()) {
                failed++;
                GeyserTranslationManager.error("item.register_failed", branch, itemType, model);
                continue;
            }
            JsonObject variant = branch.getAsJsonObject();
            try {
                ConvertedVariant result = convertVariant(model, variant, displayBlocks, branchPriority);
                converted.add(result);
                contextual |= result.definition().predicates().stream().anyMatch(GeyserItemPredicates::requiresSession);
            } catch (UnsupportedOperationException exception) {
                skipped++;
                GeyserTranslationManager.warn("item.variant_skipped", variant.get(BEDROCK_IDENTIFIER), model, exception.getMessage());
            } catch (RuntimeException exception) {
                failed++;
                GeyserTranslationManager.error("item.register_failed", exception, variant.get(BEDROCK_IDENTIFIER), itemType, model);
            }
        }
        return new ConvertedItem(itemType, converted, contextual, placesBlock, GeyserItemComponentPatches.read(entry, this.sourceVersion), skipped, failed);
    }

    private static ConvertedVariant convertVariant(Identifier model, JsonObject variant, Map<String, String> displayBlocks, int priority) {
        JsonObject resources = variant.get(RESOURCES).getAsJsonObject();
        if (resources.has(UNSUPPORTED_REASON)) {
            throw new UnsupportedOperationException(resources.get(UNSUPPORTED_REASON).getAsString());
        }
        if (!resources.has(GENERATED) || !resources.get(GENERATED).getAsBoolean()) {
            throw new UnsupportedOperationException(GeyserTranslationManager.message("item.reason.missing_resource"));
        }
        Identifier id = Identifier.of(variant.get(BEDROCK_IDENTIFIER).getAsString());
        CustomItemDefinition definition = buildDefinition(model, variant, id, id, displayBlocks, priority);
        CustomItemDefinition head = null;
        if (variant.has(HEAD_IDENTIFIER)) {
            Identifier headId = Identifier.of(variant.get(HEAD_IDENTIFIER).getAsString());
            head = buildDefinition(model, variant, headId, id, displayBlocks, 0);
        }
        return new ConvertedVariant(definition, head, tintSources(resources));
    }

    private static Map<String, Integer> tintSources(JsonObject resources) {
        JsonObject sources = resources.getAsJsonObject(TINT_SOURCES);
        if (sources == null || sources.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> binding = HashMap.newHashMap(sources.size());
        sources.entrySet().forEach(source -> binding.put(source.getKey(), source.getValue().getAsInt()));
        return Map.copyOf(binding);
    }

    /*
     * { // entry
     *   "model": "example:sword", // Java 物品模型定义 ID
     *   "components": { // 各分支共用的组件
     *     "minecraft:max_stack_size": 1 // 最大堆叠数量
     *   },
     *   "variants": [{ // variant 是其中一个分支
     *     "bedrock_identifier": "ce:example/sword", // 分支基岩物品 ID
     *     "conditions": [], // 匹配条件，空数组表示无条件
     *     "resources": { // 分支显示资源
     *       "display_block": "ce/example/sword", // 可选，缺省使用分支 ID 对应的图标
     *       "generated": true // 图标或展示方块已生成
     *     }
     *   }]
     * }
     */
    private static CustomItemDefinition buildDefinition(Identifier model, JsonObject variant, Identifier bedrockId, Identifier iconId, Map<String, String> displayBlocks, int priority) {
        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockId, model);
        builder.priority(priority);
        applyConditions(builder, variant.getAsJsonArray(CONDITIONS));
        if (!bedrockId.equals(iconId)) {
            // 先让 Geyser 匹配耐久等条件；只有已选中的物品进入头槽时才替换其显示 ID。
            builder.predicate(context -> false);
        }
        if (variant.has(WORLD_VIEW)) {
            String view = variant.get(WORLD_VIEW).getAsString();
            builder.predicate(context -> GeyserWorldItemViews.matches(view));
        }
        applyResources(builder, iconId, variant.getAsJsonObject(RESOURCES), displayBlocks);
        return builder.build();
    }

    private static void applyConditions(CustomItemDefinition.Builder builder, JsonArray conditions) {
        // 注册计划已对各显示位置的物品栈条件求交。Geyser 匹配时要求这些条件同时成立，才能使用此变体的 ID
        builder.predicateStrategy(PredicateStrategy.AND);
        for (JsonElement condition : conditions) {
            builder.predicate(GeyserItemPredicates.read(condition.getAsJsonObject()));
        }
    }

    private static void applyResources(CustomItemDefinition.Builder builder, Identifier bedrockId, JsonObject resources, Map<String, String> displayBlocks) {
        if (!resources.has(DISPLAY_BLOCK) || resources.has(DYE_COLOR)) {
            String icon = resources.has(ICON) ? resources.get(ICON).getAsString() : bedrockId.toString();
            CustomItemIcons.Builder icons = CustomItemIcons.builder().defaultIcon(icon);
            CustomItemBedrockOptions.Builder options = CustomItemBedrockOptions.builder();
            if (resources.has(DYE_COLOR)) {
                String dyedIcon = resources.has(DYED_ICON) ? resources.get(DYED_ICON).getAsString() : dyedIconKey(bedrockId.toString());
                icons.dyedIcon(dyedIcon);
                options.dyeable(resources.get(DYE_COLOR).getAsInt());
            }
            builder.bedrockOptions(options.icons(icons.build()));
            if (!resources.has(DISPLAY_BLOCK)) {
                return;
            }
        }
        String name = resources.get(DISPLAY_BLOCK).getAsString();
        String block = displayBlocks.get(name);
        if (block == null) {
            throw new IllegalStateException("Display block was not registered: " + name);
        }
        // Geyser 的公开接口不允许原版映射设置 BLOCK_PLACER；此入口与其自定义头颅共用。
        GeyserCustomItemDefinitionBuilderProxy.INSTANCE.geyserComponent(
                builder,
                GeyserItemDataComponents.BLOCK_PLACER,
                GeyserBlockPlacer.of(Identifier.of(block), !resources.has(DYE_COLOR))
        );
    }

    public record ConvertedItem(Identifier itemType, List<ConvertedVariant> variants, boolean contextual, boolean placesBlock,
                                @Nullable GeyserItemComponentPatch componentPatch, int skipped, int failed) {
    }

    public record ConvertedVariant(CustomItemDefinition definition, @Nullable CustomItemDefinition head, Map<String, Integer> tintSources) {
    }
}
