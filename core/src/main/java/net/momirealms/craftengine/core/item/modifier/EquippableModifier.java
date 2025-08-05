package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class EquippableModifier<I> implements SimpleNetworkItemDataModifier<I> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final EquipmentData data;

    public EquippableModifier(EquipmentData data) {
        this.data = data;
    }

    public EquipmentData data() {
        return data;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.EQUIPPABLE;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        return item.equippable(this.data);
    }

    @Override
    public @Nullable Key componentType(Item<I> item, ItemBuildContext context) {
        return ComponentKeys.EQUIPPABLE;
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "equippable");
            return new EquippableModifier<>(EquipmentData.fromMap(data));
        }
    }
}
