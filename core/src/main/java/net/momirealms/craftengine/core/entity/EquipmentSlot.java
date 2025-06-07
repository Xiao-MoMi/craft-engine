package net.momirealms.craftengine.core.entity;

public enum EquipmentSlot {
    HEAD,
    CHEST,
    LEGS,
    FEET,
    BODY,
    MAIN_HAND,
    OFF_HAND,
    SADDLE;

    public boolean isHand() {
        return this == MAIN_HAND || this == OFF_HAND;
    }

    public boolean isPlayerArmor() {
        return this == HEAD || this == CHEST || this == LEGS || this == FEET;
    }
}
