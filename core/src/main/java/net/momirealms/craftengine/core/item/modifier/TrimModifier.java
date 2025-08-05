package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.item.data.Trim;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

public class TrimModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key material;
    private final Key pattern;

    public TrimModifier(Key material, Key pattern) {
        this.material = material;
        this.pattern = pattern;
    }

    public Key material() {
        return material;
    }

    public Key pattern() {
        return pattern;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.TRIM;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        return item.trim(new Trim(this.pattern, this.material));
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.TRIM;
    }

    @Override
    public @Nullable Object[] nbtPath(Item<I> item, ItemBuildContext context) {
        return new Object[]{"Trim"};
    }

    @Override
    public String nbtPathString(Item<I> item, ItemBuildContext context) {
        return "Trim";
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "trim");
            String material = data.get("material").toString().toLowerCase(Locale.ENGLISH);
            String pattern = data.get("pattern").toString().toLowerCase(Locale.ENGLISH);
            return new TrimModifier<>(Key.of(material), Key.of(pattern));
        }
    }
}
