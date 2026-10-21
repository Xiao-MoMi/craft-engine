package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class ItemBindingResolver {
    private final Map<Key, ItemDefinitionBinding> definitions;
    private final ItemBehaviorReader behaviorReader;
    private final ItemRegistrationReader registrationReader;

    public ItemBindingResolver(Map<Key, ItemDefinitionBinding> definitions,
                               ItemBehaviorReader behaviorReader,
                               ItemRegistrationReader registrationReader) {
        this.definitions = Map.copyOf(definitions);
        this.behaviorReader = behaviorReader;
        this.registrationReader = registrationReader;
    }

    public ResolvedItemBinding resolve(Item item, @Nullable Player player, ItemPacketSource source) {
        Item clientItem = item.copy().toClientSide(player, source);
        return resolve(item, clientItem);
    }

    public ResolvedItemBinding resolve(Item serverItem, Item clientItem) {
        Key definitionId = VersionHelper.isOrAbove1_21_2
                ? clientItem.itemModel().map(Key::of).orElse(null)
                : clientItem.vanillaId();
        ItemDefinitionBinding definition = definitionId == null ? null : this.definitions.get(definitionId);
        return new ResolvedItemBinding(
                serverItem.id(),
                serverItem.vanillaId(),
                clientItem.vanillaId(),
                definitionId,
                definition,
                this.behaviorReader.read(clientItem),
                this.registrationReader.read(clientItem)
        );
    }
}
