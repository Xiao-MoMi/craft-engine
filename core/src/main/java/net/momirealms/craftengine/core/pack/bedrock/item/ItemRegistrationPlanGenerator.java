package net.momirealms.craftengine.core.pack.bedrock.item;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.util.ArrayList;
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
        List<ResolvedItemBinding> bindings = new ObjectArrayList<>(items.size());
        List<ItemRegistrationPlan.Diagnostic> failures = new ObjectArrayList<>();
        List<ItemDefinition> orderedItems = new ObjectArrayList<>(items);
        orderedItems.sort(Comparator.comparing(item -> item.id().asString()));
        for (ItemDefinition definition : orderedItems) {
            try {
                Item item = definition.buildItem(ItemBuildContext.empty());
                bindings.add(resolver.resolve(item, null, ItemPacketSource.CONTAINER));
            } catch (RuntimeException exception) {
                failures.add(new ItemRegistrationPlan.Diagnostic(
                        ItemRegistrationPlan.Severity.ERROR,
                        "ITEM_BINDING_FAILED",
                        definition.material(),
                        null,
                        List.of(definition.id()),
                        List.of(exception.toString())
                ));
            }
        }
        ItemRegistrationPlan plan = ItemRegistrationPlanCompiler.compile(bindings);
        if (failures.isEmpty()) {
            return plan;
        }
        List<ItemRegistrationPlan.Diagnostic> diagnostics = new ArrayList<>(plan.diagnostics());
        diagnostics.addAll(failures);
        return new ItemRegistrationPlan(plan.entries(), diagnostics);
    }
}
