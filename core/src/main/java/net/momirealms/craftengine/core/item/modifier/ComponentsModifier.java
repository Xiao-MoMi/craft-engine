package net.momirealms.craftengine.core.item.modifier;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.snbt.TagParser;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComponentsModifier<I> implements ItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final List<Pair<Key, Tag>> arguments;
    private CompoundTag customData = null;

    public ComponentsModifier(Map<String, Object> arguments) {
        List<Pair<Key, Tag>> pairs = new ArrayList<>(arguments.size());
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            Key key = Key.of(entry.getKey());
            if (key.equals(DataComponentKeys.CUSTOM_DATA)) {
                this.customData = (CompoundTag) parseValue(entry.getValue());
            } else {
                pairs.add(new Pair<>(key, parseValue(entry.getValue())));
            }
        }
        this.arguments = pairs;
    }

    public List<Pair<Key, Tag>> components() {
        return arguments;
    }

    private Tag parseValue(Object value) {
        if (value instanceof String string) {
            if (string.startsWith("(json) ")) {
                return CraftEngine.instance().platform().jsonToSparrowNBT(GsonHelper.get().fromJson(string.substring("(json) ".length()), JsonElement.class));
            } else if (string.startsWith("(snbt) ")) {
                String snbt = string.substring("(snbt) ".length());
                try {
                    return TagParser.parseCompoundFully(snbt);
                } catch (CommandSyntaxException e) {
                    throw new LocalizedResourceConfigException("warning.config.type.snbt.invalid_syntax", e.getMessage());
                }
            }
        }
        return CraftEngine.instance().platform().javaToSparrowNBT(value);
    }

    @Override
    public Key type() {
        return ItemDataModifiers.COMPONENTS;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        for (Pair<Key, Tag> entry : this.arguments) {
            item.setNBTComponent(entry.left(), entry.right());
        }
        if (this.customData != null) {
            CompoundTag tag = (CompoundTag) item.getTag(DataComponentKeys.CUSTOM_DATA);
            if (tag != null) {
                for (Map.Entry<String, Tag> entry : this.customData.entrySet()) {
                    tag.put(entry.getKey(), entry.getValue());
                }
                item.setComponent(DataComponentKeys.CUSTOM_DATA, tag);
            } else {
                item.setComponent(DataComponentKeys.CUSTOM_DATA, this.customData);
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

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "components");
            return new ComponentsModifier<>(data);
        }
    }
}
