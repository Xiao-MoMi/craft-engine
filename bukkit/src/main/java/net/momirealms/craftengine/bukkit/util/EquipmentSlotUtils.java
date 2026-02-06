package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;

public final class EquipmentSlotUtils {
    private EquipmentSlotUtils() {}

    public static Object toNMSEquipmentSlot(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case MAINHAND -> EquipmentSlotProxy.MAINHAND;
            case OFFHAND -> EquipmentSlotProxy.OFFHAND;
            case FEET -> EquipmentSlotProxy.FEET;
            case LEGS -> EquipmentSlotProxy.LEGS;
            case CHEST -> EquipmentSlotProxy.CHEST;
            case HEAD -> EquipmentSlotProxy.HEAD;
            case BODY -> EquipmentSlotProxy.BODY;
            case SADDLE -> EquipmentSlotProxy.SADDLE;
        };
    }

    public static EquipmentSlot fromNMSEquipmentSlot(Object equipmentSlot) {
        Enum<?> directionEnum = (Enum<?>) equipmentSlot;
        int index = directionEnum.ordinal();
        return EquipmentSlot.values()[index];
    }
}
