package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan.Entry;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ItemRegistrationPlanGenerator {

    private ItemRegistrationPlanGenerator() {
    }

    public static ItemRegistrationPlan generate(JavaResourcePack pack,
                                                Collection<ItemDefinition> items,
                                                ItemBehaviorReader behaviorReader,
                                                ItemRegistrationReader registrationReader) throws IOException {
        Map<Key, ItemDefinitionBinding> definitions = ItemBindingCompiler.compile(pack);
        ItemBindingResolver resolver = new ItemBindingResolver(definitions, behaviorReader, registrationReader);
        List<Entry> entries = new ObjectArrayList<>(items.size());
        List<ItemDefinition> orderedItems = new ObjectArrayList<>(items);
        orderedItems.sort(Comparator.comparing(item -> item.id().asString()));
        for (ItemDefinition definition : orderedItems) {
            try {
                Item item = definition.buildItem(ItemBuildContext.empty());
                ResolvedItemBinding binding = resolver.resolve(item, null, ItemPacketSource.CONTAINER);
                if (binding.itemModel() == null || binding.definition() == null) {
                    continue;
                }
                entries.add(createEntry(binding));
            } catch (Throwable t) {
                TranslationManager.warn("resource_pack.bedrock.item.binding", definition.id().asString(), definition.material().asString(), t.toString());
            }
        }
        return new ItemRegistrationPlan(entries);
    }

    private static Entry createEntry(ResolvedItemBinding binding) {
        // 使用原始物品 ID，避免 Java 模型混淆改变基岩资源名称。
        Key itemId = binding.itemId();
        Key bedrockId = Key.ce(itemId.namespace() + '/' + itemId.value());
        return new Entry(
                bedrockId,
                binding.clientItemType(),
                binding.itemModel(),
                binding.definition(),
                binding.clientProperties(),
                binding
        );
    }
}
