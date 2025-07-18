package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;

public class EnchantmentModifier<I> implements ItemDataModifier<I> {
    private final List<Enchantment> enchantments;

    public EnchantmentModifier(List<Enchantment> enchantments) {
        this.enchantments = enchantments;
    }

    @Override
    public String name() {
        return "enchantment";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        if (item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK)) {
            item.setStoredEnchantments(this.enchantments);
        } else {
            item.setEnchantments(this.enchantments);
        }
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK)) {
            if (VersionHelper.isOrAbove1_20_5()) {
                Tag previous = item.getSparrowNBTComponent(ComponentKeys.STORED_ENCHANTMENTS);
                if (previous != null) {
                    networkData.put(ComponentKeys.STORED_ENCHANTMENTS.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put(ComponentKeys.STORED_ENCHANTMENTS.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
            } else {
                Tag previous = item.getTag("StoredEnchantments");
                if (previous != null) {
                    networkData.put("StoredEnchantments", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put("StoredEnchantments", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
            }
        } else {
            if (VersionHelper.isOrAbove1_20_5()) {
                Tag previous = item.getSparrowNBTComponent(ComponentKeys.ENCHANTMENTS);
                if (previous != null) {
                    networkData.put(ComponentKeys.ENCHANTMENTS.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put(ComponentKeys.ENCHANTMENTS.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
            } else {
                Tag previous = item.getTag("Enchantments");
                if (previous != null) {
                    networkData.put("Enchantments", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
                } else {
                    networkData.put("Enchantments", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
                }
            }
        }
        return item;
    }
}
