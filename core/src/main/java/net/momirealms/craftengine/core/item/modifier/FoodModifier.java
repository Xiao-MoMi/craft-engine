package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FoodModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final int nutrition;
    private final float saturation;
    private final boolean canAlwaysEat;

    public FoodModifier(int nutrition, float saturation, boolean canAlwaysEat) {
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
    public Key type() {
        return ItemDataModifiers.FOOD;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.setJavaComponent(ComponentKeys.FOOD, Map.of(
                "nutrition", this.nutrition,
                "saturation", this.saturation,
                "can_always_eat", this.canAlwaysEat
        ));
        return item;
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.FOOD;
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "food");
            int nutrition = ResourceConfigUtils.getAsInt(data.get("nutrition"), "nutrition");
            float saturation = ResourceConfigUtils.getAsFloat(data.get("saturation"), "saturation");
            return new FoodModifier<>(nutrition, saturation, ResourceConfigUtils.getAsBoolean(data.getOrDefault("can-always-eat", false), "can-always-eat"));
        }
    }
}
