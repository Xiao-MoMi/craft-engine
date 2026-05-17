package net.momirealms.craftengine.core.item.recipe.transform;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.value.Enchantment;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MergeEnchantments implements ItemTransformDataProcessor {
    public static final MergeEnchantments INSTANCE = new MergeEnchantments();
    public static final ItemTransformDataProcessor.Factory<MergeEnchantments> FACTORY = new Factory();

    @Override
    public void accept(Item item1, Item item2, Item item3) {
        item1.enchantments().ifPresent(e1 -> {
            item3.enchantments().ifPresent(e2 -> {
                item3.setEnchantments(Stream.concat(e1.stream(), e2.stream())
                        .collect(Collectors.toMap(
                                Enchantment::id,
                                enchantment -> enchantment,
                                (existing, replacement) ->
                                        existing.level() > replacement.level() ? existing : replacement
                        ))
                        .values()
                        .stream()
                        .toList());
            });
        });
    }

    private static class Factory implements ItemTransformDataProcessor.Factory<MergeEnchantments> {

        @Override
        public MergeEnchantments create(ConfigSection section) {
            return INSTANCE;
        }
    }
}