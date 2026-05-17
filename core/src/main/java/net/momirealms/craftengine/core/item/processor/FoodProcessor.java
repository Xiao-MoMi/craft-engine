package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public final class FoodProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<FoodProcessor> FACTORY = new Factory();
    private final int nutrition;
    private final float saturation;
    private final boolean canAlwaysEat;

    public FoodProcessor(int nutrition, float saturation, boolean canAlwaysEat) {
        this.canAlwaysEat = canAlwaysEat;
        this.nutrition = nutrition;
        this.saturation = saturation;
    }

    public boolean canAlwaysEat() {
        return canAlwaysEat;
    }

    public int nutrition() {
        return nutrition;
    }

    public float saturation() {
        return saturation;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        item.setJavaComponent(DataComponentKeys.FOOD, Map.of(
                "nutrition", this.nutrition,
                "saturation", this.saturation,
                "can_always_eat", this.canAlwaysEat
        ));
        return item;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.FOOD;
    }

    private static class Factory implements ItemProcessorFactory<FoodProcessor> {
        private static final String[] CAN_ALWAYS_EAT = new String[]{"can_always_eat", "can-always-eat"};

        @Override
        public FoodProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            return new FoodProcessor(
                    section.getInt("nutrition"),
                    section.getFloat("saturation"),
                    section.getBoolean(CAN_ALWAYS_EAT)
            );
        }
    }
}
