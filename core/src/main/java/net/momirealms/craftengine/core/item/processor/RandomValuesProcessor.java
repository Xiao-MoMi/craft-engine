package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.NamedRandoms;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RandomValuesProcessor implements ItemProcessor {
    public static final String TAG_PATH = "craftengine:random_values";
    public static final ItemProcessorFactory<RandomValuesProcessor> FACTORY = new Factory();
    private final Map<String, NumberProvider> values;

    public RandomValuesProcessor(Map<String, NumberProvider> values) {
        this.values = values;
    }

    @Override
    public void apply(ItemBuildContext context) {
        Map<String, Double> rolled = null;
        Item item = context.item();
        NamedRandoms existingRandoms = item.getCustomData(NamedRandoms.class, TAG_PATH);
        Map<String, Double> existing = existingRandoms == null ? Map.of() : existingRandoms.values;
        for (Map.Entry<String, NumberProvider> entry : this.values.entrySet()) {
            if (existing.containsKey(entry.getKey())) continue;
            if (rolled == null) rolled = new LinkedHashMap<>(existing);
            rolled.put(entry.getKey(), entry.getValue().getDouble(context));
        }
        if (rolled != null) {
            item.setCustomData(new NamedRandoms(rolled), TAG_PATH);
        }
    }

    private static class Factory implements ItemProcessorFactory<RandomValuesProcessor> {

        @Override
        public RandomValuesProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            Map<String, NumberProvider> values = new LinkedHashMap<>();
            for (String key : section.keySet()) {
                values.put(key, section.getNonNullNumber(key));
            }
            return new RandomValuesProcessor(values);
        }
    }
}
