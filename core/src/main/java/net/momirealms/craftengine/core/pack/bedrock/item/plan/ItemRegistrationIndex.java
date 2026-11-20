package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ItemRegistrationIndex {
    // 按 Java 客户端收到的物品类型和 item_model 查找该组合首次注册的基岩物品 ID，供后续绑定复用。
    private final Map<ClientModel, Key> models;
    // 记录所有已占用的基岩物品 ID，包括共用同一模型组合的不同物品 ID，用于检查注册冲突。
    private final Set<Key> identifiers;

    ItemRegistrationIndex(List<ResolvedItemBinding> explicitItems) {
        this.models = HashMap.newHashMap(explicitItems.size());
        this.identifiers = HashSet.newHashSet(explicitItems.size());
        for (ResolvedItemBinding item : explicitItems) {
            this.models.putIfAbsent(new ClientModel(item.clientItemType(), item.itemModel()), item.bedrockId());
            this.identifiers.add(item.bedrockId());
        }
    }

    Key identifier(Key itemType, Key itemModel) {
        return this.models.get(new ClientModel(itemType, itemModel));
    }

    boolean containsModel(Key itemType, Key itemModel) {
        return this.models.containsKey(new ClientModel(itemType, itemModel));
    }

    boolean add(ResolvedItemBinding item) {
        if (!this.identifiers.add(item.bedrockId())) {
            return false;
        }
        this.models.putIfAbsent(new ClientModel(item.clientItemType(), item.itemModel()), item.bedrockId());
        return true;
    }

    private record ClientModel(Key itemType, Key itemModel) {
    }
}
