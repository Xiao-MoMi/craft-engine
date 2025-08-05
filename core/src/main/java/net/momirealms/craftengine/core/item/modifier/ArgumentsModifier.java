package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.StringTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ArgumentsModifier<I> implements ItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    public static final String ARGUMENTS_TAG = "craftengine:arguments";
    private final Map<String, TextProvider> arguments;

    public ArgumentsModifier(Map<String, TextProvider> arguments) {
        this.arguments = arguments;
    }

    public Map<String, TextProvider> arguments() {
        return arguments;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.ARGUMENTS;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        if (VersionHelper.isOrAbove1_20_5()) {
            CompoundTag customData = (CompoundTag) Optional.ofNullable(item.getSparrowNBTComponent(ComponentKeys.CUSTOM_DATA)).orElse(new CompoundTag());
            CompoundTag argumentTag = new CompoundTag();
            for (Map.Entry<String, TextProvider> entry : this.arguments.entrySet()) {
                argumentTag.put(entry.getKey(), new StringTag(entry.getValue().get(context)));
            }
            customData.put(ARGUMENTS_TAG, argumentTag);
            item.setNBTComponent(ComponentKeys.CUSTOM_DATA, customData);
        } else {
            Map<String, String> processed = new HashMap<>();
            for (Map.Entry<String, TextProvider> entry : this.arguments.entrySet()) {
                processed.put(entry.getKey(), entry.getValue().get(context));
            }
            item.setTag(processed, ARGUMENTS_TAG);
        }
        return item;
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "arguments");
            Map<String, TextProvider> arguments = new HashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                arguments.put(entry.getKey(), TextProviders.fromString(entry.getValue().toString()));
            }
            return new ArgumentsModifier<>(arguments);
        }
    }
}
