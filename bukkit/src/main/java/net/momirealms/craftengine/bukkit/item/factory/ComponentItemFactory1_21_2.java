package net.momirealms.craftengine.bukkit.item.factory;

import net.momirealms.craftengine.bukkit.item.ComponentItemWrapper;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.item.DataComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.item.component.UseRemainderProxy;

import java.util.Locale;
import java.util.Map;
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
        return item.getJavaComponent(DataComponentTypes.TOOLTIP_STYLE);
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
        return item.getJavaComponent(DataComponentTypes.ITEM_MODEL);
    }

    @Override
    protected void useRemainder(ComponentItemWrapper item, Item data, int count) {
        data.count(count);
        Object useRemainder;
        if (VersionHelper.isOrAbove26_1()) {
            useRemainder = UseRemainderProxy.INSTANCE.newInstance$1(ItemStackUtils.toItemStackTemplate(data));
        } else {
            useRemainder = UseRemainderProxy.INSTANCE.newInstance$0(data.getMinecraftItem());
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
        Optional<Object> optionalData = item.getJavaComponent(DataComponentTypes.EQUIPPABLE);
        if (optionalData.isEmpty()) return Optional.empty();
        Map<String, Object> data = MiscUtils.castToMap(optionalData.get());
        String slot = data.get("slot").toString();
        return Optional.of(new EquipmentData(
                EquipmentSlot.valueOf(slot.toUpperCase(Locale.ROOT)),
                data.containsKey("asset_id") ? Key.of((String) data.get("asset_id")) : null,
                (boolean) data.getOrDefault("dispensable", true),
                (boolean) data.getOrDefault("swappable", true),
                (boolean) data.getOrDefault("damage_on_hurt", true),
                (boolean) data.getOrDefault("equip_on_interact", false),
                (boolean) data.getOrDefault("can_be_sheared", false),
                data.containsKey("camera_overlay") ? Key.of((String) data.get("camera_overlay")) : null
        ));
    }
}