package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Map;

public class FoodModifier<I> implements ItemDataModifier<I> {
    private final int nutrition;
    private final float saturation;
    private final boolean canAlwaysEat;

    public FoodModifier(int nutrition, float saturation, boolean canAlwaysEat) {
        this.canAlwaysEat = canAlwaysEat;
        this.nutrition = nutrition;
        this.saturation = saturation;
    }

    @Override
    public String name() {
        return "food";
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
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        Tag previous = item.getSparrowNBTComponent(ComponentKeys.FOOD);
        if (previous != null) {
            networkData.put(ComponentKeys.FOOD.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
        } else {
            networkData.put(ComponentKeys.FOOD.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
        }
        return item;
    }
}
