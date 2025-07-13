package net.momirealms.craftengine.bukkit.item.factory;

import net.momirealms.craftengine.bukkit.item.ComponentItemWrapper;
import net.momirealms.craftengine.bukkit.item.ComponentTypes;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

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
            item.resetComponent(ComponentTypes.TOOLTIP_STYLE);
        } else {
            item.setJavaComponent(ComponentTypes.TOOLTIP_STYLE, data);
        }
    }

    @Override
    protected Optional<String> tooltipStyle(ComponentItemWrapper item) {
        return item.getJavaComponent(ComponentTypes.TOOLTIP_STYLE);
    }

    @Override
    protected void itemModel(ComponentItemWrapper item, String data) {
        if (data == null) {
            item.resetComponent(ComponentTypes.ITEM_MODEL);
        } else {
            item.setJavaComponent(ComponentTypes.ITEM_MODEL, data);
        }
    }

    @Override
    protected Optional<String> itemModel(ComponentItemWrapper item) {
        return item.getJavaComponent(ComponentTypes.ITEM_MODEL);
    }

    @Override
    protected void equippable(ComponentItemWrapper item, EquipmentData data) {
        if (data == null) {
            item.resetComponent(ComponentTypes.EQUIPPABLE);
        } else {
            item.setSparrowNBTComponent(ComponentTypes.EQUIPPABLE, data.toNBT());
        }
    }

    @Override
    protected Optional<EquipmentData> equippable(ComponentItemWrapper item) {
        Optional<Object> optionalData = item.getJavaComponent(ComponentTypes.EQUIPPABLE);
        if (optionalData.isEmpty()) return Optional.empty();
        Map<String, Object> data = MiscUtils.castToMap(optionalData.get(), false);
        String slot = data.get("slot").toString();
        return Optional.of(new EquipmentData(
                EquipmentSlot.valueOf(slot.toUpperCase(Locale.ENGLISH)),
                data.containsKey("asset_id") ? Key.of((String) data.get("asset_id")) : null,
                (boolean) data.getOrDefault("dispensable", true),
                (boolean) data.getOrDefault("swappable", true),
                (boolean) data.getOrDefault("damage_on_hurt", true),
                (boolean) data.getOrDefault("equip_on_interact", false),
                data.containsKey("camera_overlay") ? Key.of((String) data.get("camera_overlay")) : null
        ));
    }
}