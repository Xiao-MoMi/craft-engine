package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.common.collect.MapMaker;
import com.google.common.collect.SortedSetMultimap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bedrock.geyser.item.predicate.GeyserItemPredicates;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

final class GeyserItemBindingPlan {
    final Set<String> contextual;
    final Set<String> selected;
    final boolean usesDimension;
    final Map<String, GeyserItemTints> tints;
    private final Map<String, String> heads;
    private final ConcurrentMap<ItemMappings, Bindings> byMappings = new MapMaker().weakKeys().makeMap();

    GeyserItemBindingPlan(Map<String, CustomItemDefinition> definitions, Map<String, String> heads, Map<String, GeyserItemTints> tints) {
        Set<String> contextual = new HashSet<>();
        Set<String> selectedModels = new HashSet<>();
        boolean dimension = false;
        for (CustomItemDefinition definition : definitions.values()) {
            for (MinecraftPredicate<? super ItemPredicateContext> predicate : definition.predicates()) {
                if (GeyserItemPredicates.requiresSession(predicate)) {
                    contextual.add(definition.bedrockIdentifier().toString());
                    if (GeyserItemPredicates.usesSelection(predicate)) {
                        selectedModels.add(definition.model().toString());
                    } else {
                        dimension = true;
                    }
                }
            }
        }
        Set<String> selected = new HashSet<>();
        for (CustomItemDefinition definition : definitions.values()) {
            if (selectedModels.contains(definition.model().toString())) {
                selected.add(definition.bedrockIdentifier().toString());
            }
        }
        this.contextual = Set.copyOf(contextual);
        this.selected = Set.copyOf(selected);
        this.usesDimension = dimension;
        this.heads = Map.copyOf(heads);
        this.tints = Map.copyOf(tints);
    }

    synchronized Bindings forMappings(ItemMappings original) {
        return this.byMappings.computeIfAbsent(original, this::prepare);
    }

    private Bindings prepare(ItemMappings original) {
        IntArrayList affected = new IntArrayList();
        if (!this.contextual.isEmpty()) {
            ItemMapping[] items = original.getItems();
            for (int index = 0; index < items.length; index++) {
                SortedSetMultimap<?, GeyserCustomMappingData> mappings = items[index].getCustomItemDefinitions();
                if (mappings != null) {
                    for (GeyserCustomMappingData mapping : mappings.values()) {
                        if (this.contextual.contains(mapping.definition().bedrockIdentifier().toString())) {
                            affected.add(index);
                            break;
                        }
                    }
                }
            }
        }
        return new Bindings(affected.toIntArray(), new GeyserHeadItemViews(original, this.heads));
    }

    record Bindings(int[] affected, GeyserHeadItemViews heads) {
    }
}
