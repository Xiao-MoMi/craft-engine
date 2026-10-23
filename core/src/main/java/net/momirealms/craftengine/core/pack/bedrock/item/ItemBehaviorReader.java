package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.item.Item;

public interface ItemBehaviorReader {

    ItemBehaviorDescription read(Item item, ItemRegistrationProperties properties);
}
