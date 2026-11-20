package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.pack.ExternalItemModel;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

final class ExternalItemModelBindings {

    private ExternalItemModelBindings() {
    }

    static void append(List<ResolvedItemBinding> entries,
                       ItemRegistrationIndex index,
                       Function<Key, ItemModelDefinitionBinding> definitions,
                       Map<String, List<ExternalItemModel>> contributions,
                       Function<Key, Item> itemFactory,
                       ItemBehaviorReader behaviorReader,
                       ItemRegistrationReader registrationReader) throws IOException {
        if (contributions.isEmpty()) {
            return;
        }
        Map<ExternalItemModel, String> ordered = new TreeMap<>(Comparator
                .comparing((ExternalItemModel model) -> model.baseItem().asString())
                .thenComparing(model -> model.itemModel().asString()));
        new TreeMap<>(contributions).forEach((owner, models) -> {
            for (ExternalItemModel model : models) {
                ordered.putIfAbsent(model, owner);
            }
        });
        for (Map.Entry<ExternalItemModel, String> entry : ordered.entrySet()) {
            ExternalItemModel model = entry.getKey();
            try {
                Item item = itemFactory.apply(model.baseItem());
                if (item == null || !item.vanillaId().equals(model.baseItem())) {
                    throw new IllegalArgumentException("Unknown vanilla base item: " + model.baseItem());
                }
                item.itemModel(model.itemModel().asString());
                Item clientItem = item.copy().toClientSide(null, ItemPacketSource.GENERIC);
                Key clientModel = clientItem.itemModel().map(Key::of).orElse(null);
                if (clientModel == null) {
                    continue;
                }
                if (index.containsModel(clientItem.vanillaId(), clientModel)) {
                    continue;
                }
                ItemModelDefinitionBinding definition = definitions.apply(clientModel);
                if (definition == null) {
                    throw new IllegalArgumentException("Missing item definition assets/" + clientModel.namespace() + "/items/" + clientModel.value() + ".json in the merged Java resource pack");
                }
                ItemBehaviorDescription behavior = behaviorReader.read(item, clientItem);
                ItemRegistrationProperties properties = registrationReader.read(clientItem, behavior);
                String identity = model.baseItem().asString() + '\n' + model.itemModel().asString();
                Key id = Key.of("external", UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString());
                ResolvedItemBinding binding = new ResolvedItemBinding(id, model.baseItem(), clientItem.vanillaId(), clientModel, definition, behavior, properties, false);
                if (!index.add(binding)) {
                    throw new IllegalArgumentException("Conflicting generated identifier: " + binding.bedrockId());
                }
                entries.add(binding);
            } catch (RuntimeException exception) {
                throw new IOException("Failed to register external item model from " + entry.getValue() + ": " + model.baseItem() + " + " + model.itemModel() + ": " + exception.getMessage(), exception);
            }
        }
    }
}
