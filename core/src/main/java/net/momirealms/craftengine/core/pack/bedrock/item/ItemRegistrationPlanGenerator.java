package net.momirealms.craftengine.core.pack.bedrock.item;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.pack.bedrock.item.ItemRegistrationPlan.Entry;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        List<ItemRegistrationPlan.Diagnostic> failures = new ObjectArrayList<>();
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
                failures.add(new ItemRegistrationPlan.Diagnostic(
                        ItemRegistrationPlan.Severity.ERROR,
                        "ITEM_BINDING_FAILED",
                        definition.material(),
                        null,
                        List.of(definition.id()),
                        List.of(t.toString())
                ));
            }
        }
        return new ItemRegistrationPlan(entries, failures);
    }

    @SuppressWarnings("all")
    private static Entry createEntry(ResolvedItemBinding binding) {
        // 注册身份只由基础 Java 物品和 item_model 决定，组件与行为修改不改变标识符。
        String identity = binding.clientItemType().asString() + "|" + binding.itemModel().asString();
        String token = HashUtils.sha1(identity.getBytes(StandardCharsets.UTF_8));
        Key bedrockId = Key.ce(token);
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
