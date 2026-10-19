package net.momirealms.craftengine.core.pack.bedrock.item;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.model.definition.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.definition.analysis.ItemModelProgram;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class BedrockItemSources {
    private final List<BedrockItemSource> items;
    private final Map<Key, ItemModelProgram> definitions = new LinkedHashMap<>();
    private final Map<Key, List<BedrockItemSource>> byModel = new LinkedHashMap<>();

    public BedrockItemSources(JavaResourcePack pack, Collection<BedrockItemSource> items) throws IOException {
        List<Key> resources = pack.itemDefinitions();
        Set<Key> requested = new TreeSet<>(Comparator.comparing(Key::asString));
        requested.addAll(resources);
        List<BedrockItemSource> ordered = new ArrayList<>(items);
        Set<Key> ids = new HashSet<>();
        for (BedrockItemSource item : items) {
            ids.add(item.id());
            requested.add(item.itemModel());
        }
        for (Key resource : resources) {
            if (!ids.contains(resource) && CraftEngine.instance().itemManager().isVanillaItem(resource)) {
                ordered.add(new BedrockItemSource(resource, resource, resource));
            }
        }
        ordered.sort(Comparator.comparing(item -> item.id().asString()));
        this.items = ordered;
        for (BedrockItemSource item : ordered) {
            this.byModel.computeIfAbsent(item.itemModel(), key -> new ObjectArrayList<>(1)).add(item);
        }
        for (Key resource : requested) {
            try {
                ModernItemModel definition = pack.readModernItemModel(resource);
                if (definition == null) {
                    CraftEngine.instance().logger().warn("Missing Bedrock item definition model: " + resource);
                    continue;
                }
                this.definitions.put(resource, ItemModelProgram.compile(definition));
            } catch (IOException | RuntimeException exception) {
                CraftEngine.instance().logger().warn("Skipping Bedrock item definition " + resource + ": " + exception.getMessage());
            }
        }
    }

    public List<BedrockItemSource> items() {
        return this.items;
    }

    public Map<Key, ItemModelProgram> definitions() {
        return this.definitions;
    }

    public List<BedrockItemSource> items(Key model) {
        return this.byModel.getOrDefault(model, List.of());
    }
}
