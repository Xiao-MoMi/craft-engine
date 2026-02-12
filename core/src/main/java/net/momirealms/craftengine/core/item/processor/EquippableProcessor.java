package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.DataComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemProcessorFactory;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class EquippableProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<EquippableProcessor> FACTORY = new Factory();
    private final EquipmentData data;

    public EquippableProcessor(EquipmentData data) {
        this.data = data;
    }

    public EquipmentData data() {
        return this.data;
    }

    @Override
    public <I> Item<I> apply(Item<I> item, ItemBuildContext context) {
        return item.equippable(this.data);
    }

    @Override
    public <I> @NotNull Key componentType(Item<I> item, ItemBuildContext context) {
        return DataComponentKeys.EQUIPPABLE;
    }

    private static class Factory implements ItemProcessorFactory<EquippableProcessor> {

        @Override
        public EquippableProcessor create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "equippable");
            return new EquippableProcessor(EquipmentData.fromMap(data));
        }
    }
}
