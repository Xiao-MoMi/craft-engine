package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.item.Item;

public interface ItemRegistrationReader {

    ItemRegistrationProperties read(Item item, ItemBehaviorDescription behavior);
}
