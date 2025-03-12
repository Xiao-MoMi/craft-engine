package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Enchantment;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;

import java.util.List;

public class EnchantmentModifier<I> implements ItemModifier<I> {
    private final List<Enchantment> enchantments;

    public EnchantmentModifier(List<Enchantment> enchantments) {
        this.enchantments = enchantments;
    }

    @Override
    public String name() {
        return "enchantment";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        if (item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK)) item.setStoredEnchantments(enchantments);
        else item.setEnchantments(enchantments);
    }
}
