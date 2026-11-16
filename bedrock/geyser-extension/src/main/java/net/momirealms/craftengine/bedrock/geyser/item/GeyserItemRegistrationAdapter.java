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
        JsonArray variants = entry.getAsJsonArray(VARIANTS);
        List<CustomItemDefinition> definitions = new ArrayList<>(variants.size());
        Map<String, String> headIdentifiers = new HashMap<>();
        int priority = variants.size();
        int skipped = 0;
        int failed = 0;
        for (JsonElement branch : variants) {
            JsonObject variant = branch.getAsJsonObject();
            int branchPriority = priority--;
            try {
                JsonObject resources = variant.getAsJsonObject(RESOURCES);
                if (resources.has(UNSUPPORTED_REASON)) {
                    throw new UnsupportedOperationException(resources.get(UNSUPPORTED_REASON).getAsString());
                }
                if (!resources.has(GENERATED) || !resources.get(GENERATED).getAsBoolean()) {
                    throw new UnsupportedOperationException(GeyserTranslationManager.message("item.reason.missing_resource"));
                }
                Identifier id = Identifier.of(variant.get(BEDROCK_IDENTIFIER).getAsString());
                CustomItemDefinition definition = this.convertVariant(entry, variant, id, id, displayBlocks, branchPriority);
                // 主变体与头槽别名一起完成转换后再加入，避免留下半个分支；其他分支的优先级不变。
                CustomItemDefinition head = variant.has(HEAD_IDENTIFIER) ? this.convertVariant(entry, variant, Identifier.of(variant.get(HEAD_IDENTIFIER).getAsString()), id, displayBlocks, 0) : null;
                definitions.add(definition);
                if (head != null) {
                    definitions.add(head);
                    headIdentifiers.put(id.toString(), head.bedrockIdentifier().toString());
                }
            } catch (UnsupportedOperationException exception) {
                skipped++;
                GeyserTranslationManager.warn("item.variant_skipped", variant.get(BEDROCK_IDENTIFIER), entry.get(MODEL), exception.getMessage());
            } catch (RuntimeException exception) {
                failed++;
                GeyserTranslationManager.error("item.register_failed", exception, variant.get(BEDROCK_IDENTIFIER), entry.get(ITEM_TYPE), entry.get(MODEL));
            }
        }
        return new ConvertedItem(
                definitions,
                definitions.stream().anyMatch(definition -> definition.predicates().stream().anyMatch(GeyserItemPredicates::requiresSession)),
                GeyserItemComponentPatches.read(entry, this.sourceVersion),
                headIdentifiers, skipped, failed
        );
    }

    /*
     * { // entry
     *   "model": "example:sword", // Java 物品模型定义 ID
     *   "components": { // 各分支共用的组件
     *     "minecraft:max_stack_size": 1 // 最大堆叠数量
     *   },
     *   "variants": [{ // variant 是其中一个分支
     *     "bedrock_identifier": "craftengine:example/sword", // 分支基岩物品 ID
     *     "conditions": [], // 匹配条件，空数组表示无条件
     *     "resources": { // 分支显示资源
     *       "display_block": "craftengine/example/sword", // 可选，缺省使用分支 ID 对应的图标
     *       "generated": true // 图标或展示方块已生成
     *     }
     *   }]
     * }
     */
    private CustomItemDefinition convertVariant(JsonObject entry, JsonObject variant, Identifier bedrockId, Identifier iconId, Map<String, String> displayBlocks, int priority) {
        Identifier model = Identifier.of(entry.get(MODEL).getAsString());
        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockId, model);
        builder.priority(priority);
        applyConditions(builder, variant.getAsJsonArray(CONDITIONS));
        if (!bedrockId.equals(iconId)) {
            // 先让 Geyser 匹配耐久等条件；只有已选中的物品进入头槽时才替换其显示 ID。
            builder.predicate(context -> false);
        }
        if (variant.has("world_view")) {
            String view = variant.get("world_view").getAsString();
            builder.predicate(context -> GeyserWorldItemViews.matches(view));
        }
        applyResources(builder, iconId, variant.getAsJsonObject(RESOURCES), displayBlocks);
        return builder.build();
    }

    private static void applyConditions(CustomItemDefinition.Builder builder, JsonArray conditions) {
        builder.predicateStrategy(PredicateStrategy.AND);
        for (JsonElement condition : conditions) {
            builder.predicate(GeyserItemPredicates.read(condition.getAsJsonObject()));
        }
    }

    private static void applyResources(CustomItemDefinition.Builder builder, Identifier bedrockId, JsonObject resources, Map<String, String> displayBlocks) {
        if (!resources.has(DISPLAY_BLOCK) || resources.has(DYE_COLOR)) {
            CustomItemIcons.Builder icons = CustomItemIcons.builder().defaultIcon(bedrockId.toString());
            CustomItemBedrockOptions.Builder options = CustomItemBedrockOptions.builder();
            if (resources.has(DYE_COLOR)) {
                icons.dyedIcon(dyedIconKey(bedrockId.toString()));
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

    public record ConvertedItem(List<CustomItemDefinition> definitions, boolean contextual, @Nullable GeyserItemComponentPatch componentPatch,
                                Map<String, String> headIdentifiers, int skipped, int failed) {
    }
}
