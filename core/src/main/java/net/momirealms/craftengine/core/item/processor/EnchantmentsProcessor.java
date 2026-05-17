package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.component.value.Enchantment;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EnchantmentsProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<EnchantmentsProcessor> FACTORY = new Factory();
    private static final Object[] STORED_ENCHANTMENTS = new Object[] {"StoredEnchantments"};
    private static final Object[] ENCHANTMENTS = new Object[] {"Enchantments"};
    private final List<Enchantment> enchantments;
    private final boolean merge;

    public EnchantmentsProcessor(List<Enchantment> enchantments, boolean merge) {
        this.enchantments = enchantments;
        this.merge = merge;
    }

    public boolean merge() {
        return merge;
    }

    public List<Enchantment> enchantments() {
        return enchantments;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        if (item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK)) {
            if (this.merge) {
                Optional<List<Enchantment>> previousEnchantments = item.storedEnchantments();
                if (previousEnchantments.isPresent()) {
                    return item.setStoredEnchantments(Stream.concat(previousEnchantments.get().stream(), this.enchantments.stream())
                            .collect(Collectors.toMap(
                                    Enchantment::id,
                                    enchantment -> enchantment,
                                    (existing, replacement) ->
                                            existing.level() > replacement.level() ? existing : replacement
                            ))
                            .values()
                            .stream()
                            .toList());
                }
            }
            return item.setStoredEnchantments(this.enchantments);
        } else {
            if (this.merge) {
                Optional<List<Enchantment>> previousEnchantments = item.enchantments();
                if (previousEnchantments.isPresent()) {
                    return item.setEnchantments(Stream.concat(previousEnchantments.get().stream(), this.enchantments.stream())
                            .collect(Collectors.toMap(
                                    Enchantment::id,
                                    enchantment -> enchantment,
                                    (existing, replacement) ->
                                            existing.level() > replacement.level() ? existing : replacement
                            ))
                            .values()
                            .stream()
                            .toList());
                }
            }
            return item.setEnchantments(this.enchantments);
        }
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK) ? DataComponentKeys.STORED_ENCHANTMENTS : DataComponentKeys.ENCHANTMENTS;
    }

    @Override
    public Object[] nbtPath(Item item, ItemBuildContext context) {
        return item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK) ? STORED_ENCHANTMENTS : ENCHANTMENTS;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return item.vanillaId().equals(ItemKeys.ENCHANTED_BOOK) ? "StoredEnchantments" : "Enchantments";
    }

    private static class Factory implements ItemProcessorFactory<EnchantmentsProcessor> {

        @Override
        public EnchantmentsProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            boolean merge = false;
            ConfigSection enchantSection;
            if (section.containsKey("merge") || section.containsKey("enchantments")) {
                merge = section.getBoolean("merge");
                enchantSection = section.getNonNullSection("enchantments");
            } else {
                enchantSection = section;
            }
            List<Enchantment> enchantments = new ArrayList<>();
            for (String enchantment : enchantSection.keySet()) {
                enchantments.add(new Enchantment(Key.of(enchantment), enchantSection.getNonNullValue(enchantment, ConfigConstants.ARGUMENT_INT, v -> v.getAsInt(1, 255))));
            }
            return new EnchantmentsProcessor(enchantments, merge);
        }
    }
}
