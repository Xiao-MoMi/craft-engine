package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.util.Key;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ItemRegistrationIndex {
    private final Set<ClientModel> models;
    private final Set<Key> identifiers;

    ItemRegistrationIndex(List<ResolvedItemBinding> explicitItems) {
        this.models = HashSet.newHashSet(explicitItems.size());
        this.identifiers = HashSet.newHashSet(explicitItems.size());
        for (ResolvedItemBinding item : explicitItems) {
            this.models.add(new ClientModel(item.clientItemType(), item.itemModel()));
            this.identifiers.add(item.bedrockId());
        }
    }

    boolean containsModel(Key itemType, Key itemModel) {
        return this.models.contains(new ClientModel(itemType, itemModel));
    }

    boolean add(ResolvedItemBinding item) {
        if (!this.identifiers.add(item.bedrockId())) {
            return false;
        }
        this.models.add(new ClientModel(item.clientItemType(), item.itemModel()));
        return true;
    }

    private record ClientModel(Key itemType, Key itemModel) {
    }
}
