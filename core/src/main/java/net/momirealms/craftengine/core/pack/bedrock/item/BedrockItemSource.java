package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.processor.ItemModelProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.item.processor.OverwritableItemModelProcessor;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public record BedrockItemSource(Key id, Key javaItem, Key itemModel, @Nullable ItemDefinition definition) {

    public BedrockItemSource(Key id, Key javaItem, Key itemModel) {
        this(id, javaItem, itemModel, null);
    }

    // 收集基岩版所需的全部物品定义
    public static List<BedrockItemSource> collect(Collection<ItemDefinition> definitions, Map<Key, Key> mappings) {
        List<BedrockItemSource> items = new ArrayList<>();
        for (ItemDefinition definition : definitions) {
            if (definition.isVanillaItem()) {
                continue;
            }
            Key model = findItemModel(definition.processors(), null);
            model = findItemModel(definition.clientBoundProcessors(), model);
            if (model != null) {
                items.add(new BedrockItemSource(definition.id(), definition.clientBoundMaterial(), mappings.getOrDefault(model, model), definition));
            }
        }
        return items;
    }

    // 有些物品可能使用 data 或者 client_bound_data 直接设置 item_model
    // 所以不能仅靠根节点下的 item_model 作为唯一依据
    private static Key findItemModel(ItemProcessor[] processors, Key model) {
        for (ItemProcessor processor : processors) {
            if (processor instanceof ItemModelProcessor itemModel) {
                model = itemModel.data();
            } else if (model == null && processor instanceof OverwritableItemModelProcessor itemModel) {
                model = itemModel.data();
            }
        }
        return model;
    }
}
