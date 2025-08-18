package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDataModifierFactory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PDCModifier<I> implements ItemDataModifier<I> {
    public static final String BUKKIT_PDC = "PublicBukkitValues";
    public static final Factory<?> FACTORY = new Factory<>();
    private final CompoundTag data;

    public PDCModifier(CompoundTag data) {
        this.data = data;
    }

    @Override
    public Key type() {
        return ItemDataModifiers.PDC;
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        if (VersionHelper.isOrAbove1_20_5()) {
            CompoundTag customData = (CompoundTag) Optional.ofNullable(item.getSparrowNBTComponent(ComponentKeys.CUSTOM_DATA)).orElseGet(CompoundTag::new);
            customData.put(BUKKIT_PDC, this.data);
            item.setNBTComponent(ComponentKeys.CUSTOM_DATA, customData);
        } else {
            item.setTag(this.data, BUKKIT_PDC);
        }
        return item;
    }

    public static class Factory<I> implements ItemDataModifierFactory<I> {

        @Override
        public ItemDataModifier<I> create(Object arg) {
            Map<String, Object> data = ResourceConfigUtils.getAsMap(arg, "pdc");
            CompoundTag tag = (CompoundTag) CraftEngine.instance().platform().javaToSparrowNBT(data);
            return new PDCModifier<>(tag);
        }
    }
}
