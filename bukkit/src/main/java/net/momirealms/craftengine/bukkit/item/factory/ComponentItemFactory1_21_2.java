package net.momirealms.craftengine.bukkit.item.factory;

import net.momirealms.craftengine.bukkit.item.ComponentItemWrapper;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.setting.value.EquipmentData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.HolderSetUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.UseRemainderProxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public class ComponentItemFactory1_21_2 extends ComponentItemFactory1_21 {

    public ComponentItemFactory1_21_2(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected void tooltipStyle(ComponentItemWrapper item, String data) {
        if (data == null) {
            item.resetComponent(DataComponentTypes.TOOLTIP_STYLE);
        } else {
            item.setJavaComponent(DataComponentTypes.TOOLTIP_STYLE, data);
        }
    }

    @Override
    protected Optional<String> tooltipStyle(ComponentItemWrapper item) {
        return item.getComponentAsJava(DataComponentTypes.TOOLTIP_STYLE);
    }

    @Override
    protected void itemModel(ComponentItemWrapper item, String data) {
        if (data == null) {
            item.resetComponent(DataComponentTypes.ITEM_MODEL);
        } else {
            item.setJavaComponent(DataComponentTypes.ITEM_MODEL, data);
        }
    }

    @Override
    protected Optional<String> itemModel(ComponentItemWrapper item) {
        return item.getComponentAsJava(DataComponentTypes.ITEM_MODEL);
    }

    @Override
    protected void useRemainder(ComponentItemWrapper item, Item data, int count) {
        data.count(count);
        Object useRemainder;
        if (VersionHelper.isOrAbove26_1) {
            useRemainder = UseRemainderProxy.INSTANCE.newInstance$1(ItemStackUtils.toItemStackTemplate(data));
        } else {
            useRemainder = UseRemainderProxy.INSTANCE.newInstance$0(data.minecraftItem());
        }
        item.setExactComponent(DataComponentTypes.USE_REMAINDER, useRemainder);
    }

    @Override
    protected Optional<ComponentItemWrapper> useRemainder(ComponentItemWrapper item) {
        Object exactComponent = item.getExactComponent(DataComponentKeys.USE_REMAINDER);
        if (exactComponent != null) {
            Object itemStack = UseRemainderProxy.INSTANCE.getConvertInto(exactComponent);
            return Optional.of(wrap(itemStack));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected void equippable(ComponentItemWrapper item, EquipmentData data) {
        if (data == null) {
            item.resetComponent(DataComponentTypes.EQUIPPABLE);
        } else {
            item.setSparrowNBTComponent(DataComponentTypes.EQUIPPABLE, data.toNBT());
        }
    }

    @Override
    protected Optional<EquipmentData> equippable(ComponentItemWrapper item) {
        Optional<Tag> optionalData = item.getComponentAsSparrowTag(DataComponentTypes.EQUIPPABLE);
        if (optionalData.isEmpty() || !(optionalData.get() instanceof CompoundTag tag)) {
            return Optional.empty();
        }
        return Optional.of(new EquipmentData(
                EquipmentSlot.valueOf(tag.getString("slot").toUpperCase(Locale.ROOT)),
                tag.containsKey("asset_id") ? Key.of(tag.getString("asset_id")) : null,
                tag.getBoolean("dispensable", true),
                tag.getBoolean("swappable", true),
                tag.getBoolean("damage_on_hurt", true),
                tag.getBoolean("equip_on_interact", false),
                tag.getBoolean("can_be_sheared", false),
                tag.containsKey("camera_overlay") ? Key.of(tag.getString("camera_overlay")) : null,
                tag.containsKey("equip_sound") ? parseSound(tag.get("equip_sound")) : null,
                tag.containsKey("shearing_sound") ? parseSound(tag.get("shearing_sound")) : null,
                HolderSetUtils.fromNBT(tag.get("allowed_entities"))
        ));
    }

    private static Pair<Key, @Nullable Float> parseSound(Tag tag) {
        if (tag instanceof StringTag stringTag) {
            return Pair.of(Key.of(stringTag.value()), null);
        } else if (tag instanceof CompoundTag compoundTag) {
            String id = compoundTag.getString("sound_id");
            Float range = compoundTag.containsKey("range") ? compoundTag.getFloat("range") : null;
            return Pair.of(Key.of(id), range);
        } else return null;
    }
}
