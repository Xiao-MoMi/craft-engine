package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.item.data.Trim;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

public class TrimModifier<I> implements ItemDataModifier<I> {
    private final Key material;
    private final Key pattern;

    public TrimModifier(Key material, Key pattern) {
        this.material = material;
        this.pattern = pattern;
    }

    @Override
    public String name() {
        return "trim";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.trim(new Trim(this.pattern, this.material));
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.isOrAbove1_20_5()) {
            Tag previous = item.getSparrowNBTComponent(ComponentKeys.TRIM);
            if (previous != null) {
                networkData.put(ComponentKeys.TRIM.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(ComponentKeys.TRIM.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else {
            Tag previous = item.getTag("Trim");
            if (previous != null) {
                networkData.put("Trim", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put("Trim", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }
}
