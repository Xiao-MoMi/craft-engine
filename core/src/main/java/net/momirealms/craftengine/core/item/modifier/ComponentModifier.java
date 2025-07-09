package net.momirealms.craftengine.core.item.modifier;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComponentModifier<I> implements ItemDataModifier<I> {
    private final List<Pair<Key, Tag>> arguments;
    private CompoundTag customData = null;

    public ComponentModifier(Map<String, Object> arguments) {
        List<Pair<Key, Tag>> pairs = new ArrayList<>(arguments.size());
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            Key key = Key.of(entry.getKey());
            if (key.equals(ComponentKeys.CUSTOM_DATA)) {
                this.customData = (CompoundTag) parseValue(entry.getValue());
            } else {
                pairs.add(new Pair<>(key, parseValue(entry.getValue())));
            }
        }
        this.arguments = pairs;
    }

    public List<Pair<Key, Tag>> arguments() {
        return arguments;
    }

    private Tag parseValue(Object value) {
        if (value instanceof String string) {
            if (string.startsWith("(json) ")) {
                return CraftEngine.instance().platform().jsonToSparrowNBT(GsonHelper.get().fromJson(string.substring("(json) ".length()), JsonElement.class));
            } else if (string.startsWith("(snbt) ")) {
                return CraftEngine.instance().platform().snbtToSparrowNBT(string.substring("(snbt) ".length()));
            }
        }
        return CraftEngine.instance().platform().javaToSparrowNBT(value);
    }

    @Override
    public String name() {
        return "components";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        for (Pair<Key, Tag> entry : this.arguments) {
            item.setNBTComponent(entry.left(), entry.right());
        }
        if (this.customData != null) {
            CompoundTag tag = (CompoundTag) item.getNBTTag(ComponentKeys.CUSTOM_DATA);
            if (tag != null) {
                for (Map.Entry<String, Tag> entry : this.customData.entrySet()) {
                    tag.put(entry.getKey(), entry.getValue());
                }
                item.setComponent(ComponentKeys.CUSTOM_DATA, tag);
            } else {
                item.setComponent(ComponentKeys.CUSTOM_DATA, this.customData);
            }
        }
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        for (Pair<Key, Tag> entry : this.arguments) {
            Tag previous = item.getSparrowNBTComponent(entry.left());
            if (previous != null) {
                networkData.put(entry.left().asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(entry.left().asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }
}
