package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.equipment.TrimBasedEquipment;
import net.momirealms.craftengine.core.item.network.ItemModelMappings;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.item.setting.value.ItemEquipment;
import net.momirealms.craftengine.core.item.setting.value.SharedModelData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public final class ItemBindingResolver {
    private static final Key TRIMMABLE_ARMOR_TAG = Key.of("minecraft:trimmable_armor");
    private final Function<Key, ItemModelDefinitionBinding> definitions;
    private final ItemBehaviorReader behaviorReader;
    private final ItemRegistrationReader registrationReader;

    public ItemBindingResolver(Map<Key, ItemModelDefinitionBinding> definitions,
                               ItemBehaviorReader behaviorReader,
                               ItemRegistrationReader registrationReader) {
        this(definitions::get, behaviorReader, registrationReader);
    }

    public ItemBindingResolver(Function<Key, ItemModelDefinitionBinding> definitions,
                               ItemBehaviorReader behaviorReader,
                               ItemRegistrationReader registrationReader) {
        this.definitions = definitions;
        this.behaviorReader = behaviorReader;
        this.registrationReader = registrationReader;
    }

    public ResolvedItemBinding resolve(Item item, @Nullable Player player, ItemPacketSource source) {
        Item clientItem = item.copy().toClientSide(player, source);
        return resolve(item, clientItem);
    }

    public ResolvedItemBinding resolve(Item serverItem, Item clientItem) {
        Key itemModel = VersionHelper.isOrAbove1_21_2
                ? clientItem.itemModel().map(Key::of).orElse(null)
                : clientItem.vanillaId();
        ItemModelDefinitionBinding definition = itemModel == null ? null : this.definitions.apply(itemModel);
        return this.resolve(serverItem, clientItem, itemModel, definition);
    }

    ResolvedItemBinding resolve(Item serverItem, Item clientItem, @Nullable Key itemModel, @Nullable ItemModelDefinitionBinding definition) {
        ItemDefinition item = serverItem.getDefinitionOrNull();
        if (definition != null) {
            SharedModelData modelData = item == null ? null : item.settings().sharedModelData();
            if (modelData != null && itemModel != null && (itemModel.equals(modelData.model()) || itemModel.equals(ItemModelMappings.getMappings().get(modelData.model())))) {
                definition = definition.withModelDataId(modelData.id());
            }
        }
        ItemBehaviorDescription behavior = this.behaviorReader.read(serverItem, clientItem);
        ItemRegistrationProperties properties = this.registrationReader.read(clientItem, behavior);
        Key headTexture = properties.headTexture();
        if (definition != null && headTexture != null) {
            definition = definition.withHeadSkin(headTexture);
        }
        boolean trimmableArmor = false;
        if (item != null && item.is(TRIMMABLE_ARMOR_TAG)) {
            ItemEquipment equipment = item.settings().equipment();
            trimmableArmor = equipment == null || !(equipment.equipment() instanceof TrimBasedEquipment);
        }
        return new ResolvedItemBinding(
                serverItem.id(),
                serverItem.vanillaId(),
                clientItem.vanillaId(),
                itemModel,
                definition,
                behavior,
                properties,
                trimmableArmor
        );
    }
}
