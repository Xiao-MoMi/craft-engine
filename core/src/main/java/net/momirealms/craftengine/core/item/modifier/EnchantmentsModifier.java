package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnchantmentsModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final List<Enchantment> enchantments;

    public EnchantmentsModifier(List<Enchantment> enchantments) {
        this.enchantments = enchantments;
    }

    public List<Enchantment> enchantments() {
        return enchantments;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.ENCHANTMENTS;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        if (item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK)) {
            return item.setStoredEnchantments(this.enchantments);
        } else {
            return item.setEnchantments(this.enchantments);
        }
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK) ? ComponentKeys.STORED_ENCHANTMENTS : ComponentKeys.ENCHANTMENTS;
    }

    @Override
    public @Nullable Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK) ? new Object[]{"StoredEnchantments"} : new Object[]{"Enchantments"};
    }

    @Override
    public String nbtPathString(Item<I> item, ItemBuildContext context) {
        return item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK) ? "StoredEnchantments" : "Enchantments";
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "enchantments");
            List<Enchantment> enchantments = new ArrayList<>();
            for (Map.Entry<String, Object> e : data.entrySet()) {
                if (e.getValue() instanceof Number number) {
                    enchantments.add(new Enchantment(Key.of(e.getKey()), number.intValue()));
                }
            }
            return new EnchantmentsModifier<>(enchantments);
        }
    }
}
