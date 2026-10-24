package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.MultimapBuilder;
import net.momirealms.craftengine.bedrock.geyser.proxy.registry.type.ItemMappingsProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.session.GeyserSessionProxy;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponentMap;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GeyserItemRuntime {
    private final GeyserSession session;
    private final Set<String> selectedItems;
    private boolean selected;

    GeyserItemRuntime(GeyserSession session, Set<String> selectedItems) {
        this.session = session;
        this.selectedItems = selectedItems;
    }

    public static void install(GeyserSession session, Map<String, CustomItemDefinition> definitions) {
        Set<String> selectedModels = new HashSet<>();
        for (CustomItemDefinition definition : definitions.values()) {
            if (definition.predicates().stream().anyMatch(GeyserItemPredicates::usesSelection)) {
                selectedModels.add(definition.model().toString());
            }
        }
        Set<String> selectedItems = new HashSet<>();
        for (CustomItemDefinition definition : definitions.values()) {
            if (selectedModels.contains(definition.model().toString())) {
                selectedItems.add(definition.bedrockIdentifier().toString());
            }
        }
        GeyserItemRuntime runtime = new GeyserItemRuntime(session, selectedItems);
        session.setItemMappings(runtime.bindMappings(session.getItemMappings(), definitions));
        GeyserSelectedItemHandler.install(session, runtime);
    }

    ItemMappings bindMappings(ItemMappings original, Map<String, CustomItemDefinition> definitions) {
        ItemMapping[] items = original.getItems().clone();
        for (int i = 0; i < items.length; i++) {
            ItemMapping item = items[i];
            SortedSetMultimap<?, GeyserCustomMappingData> mappings = item.getCustomItemDefinitions();
            if (mappings != null && mappings.values().stream().anyMatch(mapping -> definitions.containsKey(mapping.definition().bedrockIdentifier().toString()))) {
                items[i] = this.bind(item, mappings, definitions);
            }
        }
        return ItemMappingsProxy.INSTANCE.copy(original, items);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <K> ItemMapping bind(ItemMapping item, SortedSetMultimap<K, GeyserCustomMappingData> source,
                                 Map<String, CustomItemDefinition> definitions) {
        SortedSetMultimap<K, GeyserCustomMappingData> mappings = MultimapBuilder.hashKeys().treeSetValues(source.valueComparator()).build();
        for (Map.Entry<K, GeyserCustomMappingData> entry : source.entries()) {
            GeyserCustomMappingData mapping = entry.getValue();
            CustomItemDefinition definition = mapping.definition();
            if (definitions.containsKey(definition.bedrockIdentifier().toString())) {
                List<MinecraftPredicate<? super ItemPredicateContext>> predicates = new ArrayList<>(definition.predicates().size());
                for (MinecraftPredicate<? super ItemPredicateContext> predicate : definition.predicates()) {
                    predicates.add(GeyserItemPredicates.bind(predicate, this::dimension, () -> this.selected));
                }
                CustomItemDefinition bound = new SessionDefinition(definition.bedrockIdentifier(), definition.displayName(),
                        definition.model(), definition.icon(), List.copyOf(predicates), definition.predicateStrategy(),
                        definition.priority(), definition.bedrockOptions(), definition.components(), definition.removedComponents());
                mapping = new GeyserCustomMappingData(bound, mapping.itemDefinition(), mapping.integerId());
            }
            mappings.put(entry.getKey(), mapping);
        }
        // 模型键可能已被平台重定位；保留原键，不能在扩展内重新创建 Adventure Key。
        return ItemMapping.builder()
                .bedrockIdentifier(item.getBedrockIdentifier()).bedrockDefinition(item.getBedrockDefinition())
                .bedrockData(item.getBedrockData()).bedrockBlockDefinition(item.getBedrockBlockDefinition())
                .translationString(item.getTranslationString()).javaItem(item.getJavaItem())
                .containsV1Mappings(item.isContainsV1Mappings()).customItemDefinitions((SortedSetMultimap) mappings).build();
    }

    String dimension() {
        Object world = GeyserSessionProxy.INSTANCE.worldName(this.session);
        return world == null ? null : world.toString();
    }

    boolean hasSelected(ItemData item) {
        return this.selectedItems.contains(item.getDefinition().getIdentifier());
    }

    ItemData selected(GeyserItemStack item) {
        // 只在当前连接的事件循环内临时切换上下文，其余翻译仍以 selected=false 执行。
        this.selected = true;
        try {
            return item.getItemData(this.session);
        } finally {
            this.selected = false;
        }
    }

    private record SessionDefinition(Identifier bedrockIdentifier, String displayName, Identifier model, String icon,
                                     List<MinecraftPredicate<? super ItemPredicateContext>> predicates,
                                     PredicateStrategy predicateStrategy, int priority, CustomItemBedrockOptions bedrockOptions,
                                     ItemDataComponentMap components, List<Identifier> removedComponents) implements CustomItemDefinition {
    }
}
