package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.component.value.Trim;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TrimProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<TrimProcessor> FACTORY = new Factory();
    private static final Object[] NBT_PATH = new Object[] {"Trim"};
    private final Key material;
    private final Key pattern;

    public TrimProcessor(Key material, Key pattern) {
        this.material = material;
        this.pattern = pattern;
    }

    public Key material() {
        return this.material;
    }

    public Key pattern() {
        return this.pattern;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        return item.trim(new Trim(this.pattern, this.material));
    }

    @Override
    public @NotNull Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.TRIM;
    }

    @Override
    public @Nullable Object[] nbtPath(Item item, ItemBuildContext context) {
        return NBT_PATH;
    }

    @Override
    public String nbtPathString(Item item, ItemBuildContext context) {
        return "Trim";
    }

    private static class Factory implements ItemProcessorFactory<TrimProcessor> {

        @Override
        public TrimProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            return new TrimProcessor(section.getIdentifier("material"), section.getIdentifier("pattern"));
        }
    }
}
