package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.geyser.proxy.item.custom.GeyserCustomItemDefinitionBuilderProxy;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.CustomItemIcons;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
        GeyserSelectedItemHandler.Variants selected = readSelectedVariants(entry, displayBlocks);
        JsonArray variants = entry.getAsJsonArray(VARIANTS);
        List<CustomItemDefinition> definitions = new ArrayList<>(variants.size());
        // 先转换整组；任一条件不受支持时跳过整组，避免 fallback 错误覆盖。
        for (JsonElement branch : variants) {
            JsonObject variant = branch.getAsJsonObject();
            JsonObject resources = variant.getAsJsonObject(RESOURCES);
            if (resources.has(UNSUPPORTED_REASON)) {
                throw new UnsupportedOperationException(resources.get(UNSUPPORTED_REASON).getAsString());
            }
            if (variants.size() > 1 && !resources.get(GENERATED).getAsBoolean()) {
                throw new UnsupportedOperationException("A model branch has no generated render resource");
            }
            definitions.add(this.convertVariant(entry, variant, displayBlocks));
        }
        return new ConvertedItem(definitions, selected);
    }

    private CustomItemDefinition convertVariant(JsonObject entry, JsonObject variant, Map<String, String> displayBlocks) {
        Identifier bedrockId = Identifier.of(variant.get(BEDROCK_IDENTIFIER).getAsString());
        Identifier model = Identifier.of(entry.get(MODEL).getAsString());
        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockId, model);
        applyConditions(builder, variant.getAsJsonArray(CONDITIONS));
        this.applyComponents(builder, entry.getAsJsonObject(COMPONENTS));
        applyResources(builder, variant.getAsJsonObject(RESOURCES), displayBlocks);
        return builder.build();
    }

    private static void applyConditions(CustomItemDefinition.Builder builder, JsonArray conditions) {
        builder.predicateStrategy(PredicateStrategy.AND);
        for (JsonElement condition : conditions) {
            builder.predicate(GeyserItemPredicates.read(condition.getAsJsonObject()));
        }
    }

    private void applyComponents(CustomItemDefinition.Builder builder, JsonObject components) {
        for (Map.Entry<String, JsonElement> component : components.entrySet()) {
            JsonElement value = component.getValue().isJsonNull() ? null : component.getValue();
            GeyserItemComponentConverter.apply(builder, component.getKey(), value, this.sourceVersion);
        }
    }

    private static void applyResources(CustomItemDefinition.Builder builder, JsonObject resources, Map<String, String> displayBlocks) {
        if (!resources.has(DISPLAY_BLOCK)) {
            CustomItemIcons icons = CustomItemIcons.builder()
                    .defaultIcon(resources.get(ICON).getAsString())
                    .build();
            builder.bedrockOptions(CustomItemBedrockOptions.builder().icons(icons));
            return;
        }
        String name = resources.get(DISPLAY_BLOCK).getAsString();
        String block = displayBlocks.get(name);
        if (block == null) {
            throw new IllegalStateException("Display block was not registered: " + name);
        }
        // Geyser 2.12 的公开接口不允许原版映射设置 BLOCK_PLACER；此入口与其自定义头颅共用。
        GeyserCustomItemDefinitionBuilderProxy.INSTANCE.geyserComponent(builder, GeyserItemDataComponents.BLOCK_PLACER,
                GeyserBlockPlacer.of(Identifier.of(block), true));
    }

    @Nullable
    private static GeyserSelectedItemHandler.Variants readSelectedVariants(JsonObject entry, Map<String, String> blocks) {
        JsonObject selected = null;
        JsonObject unselected = null;
        boolean hasSelected = false;
        for (JsonElement value : entry.getAsJsonArray(VARIANTS)) {
            JsonObject variant = value.getAsJsonObject();
            for (JsonElement condition : variant.getAsJsonArray(CONDITIONS)) {
                JsonObject match = condition.getAsJsonObject();
                if (GeyserItemPredicates.property(match).equals("selected")) {
                    hasSelected = true;
                    if (match.get(EXPECTED).getAsBoolean()) {
                        selected = variant;
                    } else {
                        unselected = variant;
                    }
                }
            }
        }
        if (!hasSelected) {
            return null;
        }
        if (entry.getAsJsonArray(VARIANTS).size() != 2 || selected == null || unselected == null
                || selected.getAsJsonArray(CONDITIONS).size() != 1 || unselected.getAsJsonArray(CONDITIONS).size() != 1) {
            throw new UnsupportedOperationException("Nested selected branches require additional runtime matching");
        }
        return new GeyserSelectedItemHandler.Variants(reference(selected, blocks), reference(unselected, blocks));
    }

    private static GeyserSelectedItemHandler.Reference reference(JsonObject variant, Map<String, String> blocks) {
        JsonElement display = variant.getAsJsonObject(RESOURCES).get(DISPLAY_BLOCK);
        return new GeyserSelectedItemHandler.Reference(variant.get(BEDROCK_IDENTIFIER).getAsString(),
                display == null ? null : blocks.get(display.getAsString()));
    }

    public record ConvertedItem(List<CustomItemDefinition> definitions, @Nullable GeyserSelectedItemHandler.Variants selected) {
    }
}
