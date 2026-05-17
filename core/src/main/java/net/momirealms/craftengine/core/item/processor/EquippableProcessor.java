package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.setting.value.EquipmentData;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

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
    public Item apply(Item item, ItemBuildContext context) {
        return item.equippable(this.data);
    }

    @Override
    public @NotNull Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.EQUIPPABLE;
    }

    private static class Factory implements ItemProcessorFactory<EquippableProcessor> {

        @Override
        public EquippableProcessor create(ConfigValue value) {
            return new EquippableProcessor(EquipmentData.fromConfig(value.getAsSection()));
        }
    }
}
