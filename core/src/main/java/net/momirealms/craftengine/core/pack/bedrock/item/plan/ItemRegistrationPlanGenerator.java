package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
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
        Map<Key, ItemModelDefinitionBinding> definitions = ItemModelBindingCompiler.compile(pack);
        ItemBindingResolver resolver = new ItemBindingResolver(definitions, behaviorReader, registrationReader);

        // 保一下注册顺序
        List<ResolvedItemBinding> entries = new ObjectArrayList<>(items.size());
        List<ItemDefinition> orderedItems = new ObjectArrayList<>(items);
        orderedItems.sort(Comparator.comparing(item -> item.id().asString()));

        for (int i = 0, size = orderedItems.size(); i < size; i++) {
            ItemDefinition definition = orderedItems.get(i);
            try {
                Item item = definition.buildItem(ItemBuildContext.empty());
                ResolvedItemBinding binding = resolver.resolve(item, null, ItemPacketSource.GENERIC);
                if (binding.itemModel() == null || binding.definition() == null) {
                    continue;
                }
                entries.add(binding);
            } catch (Throwable t) {
                TranslationManager.warn("resource_pack.bedrock.item.binding", definition.id().asString(), definition.material().asString(), t.toString());
            }
        }
        return new ItemRegistrationPlan(entries);
    }
}
