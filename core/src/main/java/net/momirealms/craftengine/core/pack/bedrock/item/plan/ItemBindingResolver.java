package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.network.ItemPacketSource;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class ItemBindingResolver {
    private final Map<Key, ItemModelDefinitionBinding> definitions;
    private final ItemBehaviorReader behaviorReader;
    private final ItemRegistrationReader registrationReader;

    public ItemBindingResolver(Map<Key, ItemModelDefinitionBinding> definitions,
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
        ItemModelDefinitionBinding definition = itemModel == null ? null : this.definitions.get(itemModel);
        ItemBehaviorDescription behavior = this.behaviorReader.read(serverItem, clientItem);
        ItemRegistrationProperties properties = this.registrationReader.read(clientItem, behavior);
        Key headTexture = properties.headTexture();
        if (definition != null && headTexture != null) {
            definition = definition.withHeadSkin(headTexture);
        }
        return new ResolvedItemBinding(
                serverItem.id(),
                serverItem.vanillaId(),
                clientItem.vanillaId(),
                itemModel,
                definition,
                behavior,
                properties
        );
    }
}
