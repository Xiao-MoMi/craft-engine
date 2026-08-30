package net.momirealms.craftengine.bukkit.entity.listener;

final class PlayerInventorySlotMapper {
    static final int LAST_SUPPORTED_SLOT = 40;

    private PlayerInventorySlotMapper() {
    }

    static int toMenuSlot(int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot > LAST_SUPPORTED_SLOT) {
            throw new IllegalArgumentException("Unsupported player inventory slot: " + inventorySlot);
        }
        if (inventorySlot < 9) {
            return inventorySlot + 36;
        }
        if (inventorySlot > 39) {
            return inventorySlot + 5;
        }
        if (inventorySlot > 35) {
            return 8 - (inventorySlot - 36);
        }
        return inventorySlot;
    }
}
