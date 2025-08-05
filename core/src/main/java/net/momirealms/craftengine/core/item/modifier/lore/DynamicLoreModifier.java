package net.momirealms.craftengine.core.item.modifier.lore;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;
import net.momirealms.craftengine.core.item.modifier.ItemDataModifiers;
import net.momirealms.craftengine.core.item.modifier.SimpleNetworkItemDataModifier;
import net.momirealms.craftengine.core.util.Key;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DynamicLoreModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    public static final String CONTEXT_TAG_KEY = "craftengine:display_context";
    private final Map<String, LoreModifier<I>> displayContexts;
    private final LoreModifier<I> defaultModifier;

    public DynamicLoreModifier(Map<String, LoreModifier<I>> displayContexts) {
        this.displayContexts = displayContexts;
        this.defaultModifier = displayContexts.values().iterator().next();
    }

    public Map<String, LoreModifier<I>> displayContexts() {
        return displayContexts;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.DYNAMIC_LORE;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        String displayContext = Optional.ofNullable(item.getJavaTag(CONTEXT_TAG_KEY)).orElse(this.defaultModifier).toString();
        LoreModifier<I> lore = this.displayContexts.get(displayContext);
        if (lore == null) {
            lore = this.defaultModifier;
        }
        return lore.apply(item, context);
    }

    @Override
    public Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.LORE;
    }

    @Override
    public Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return new Object[]{"display", "Lore"};
    }

    @Override
    public String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "display.Lore";
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {
        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, LoreModifier<I>> dynamicLore = new LinkedHashMap<>();
            if (arg instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    dynamicLore.put(entry.getKey().toString(), LoreModifier.createLoreModifier(entry.getValue()));
                }
            }
            return new DynamicLoreModifier<>(dynamicLore);
        }
    }
}
