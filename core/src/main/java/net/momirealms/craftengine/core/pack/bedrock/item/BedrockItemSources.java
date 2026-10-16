package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BedrockItemSources {
    private final List<BedrockItemSource> items;
    private final Map<Key, BedrockItemSource> byModel;

    public BedrockItemSources(Collection<BedrockItemSource> items) {
        List<BedrockItemSource> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparing(item -> item.id().asString()));
        this.items = List.copyOf(ordered);
        this.byModel = new HashMap<>();
        for (BedrockItemSource item : ordered) {
            this.byModel.putIfAbsent(item.itemModel(), item);
        }
    }

    public List<BedrockItemSource> items() {
        return this.items;
    }

    @Nullable
    public BedrockItemSource findItemByModel(Key itemModel) {
        return this.byModel.get(itemModel);
    }
}
