package net.momirealms.craftengine.core.item.setting;

import net.momirealms.craftengine.core.item.equipment.Equipment;
import net.momirealms.craftengine.core.util.Tristate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemEquipment {
    private final Tristate clientBoundModel;
    private final Equipment equipment;
    private final EquipmentData equipmentData;

    public ItemEquipment(Tristate clientBoundModel, @Nullable EquipmentData equipmentData, Equipment equipment) {
        this.clientBoundModel = clientBoundModel;
        this.equipment = equipment;
        this.equipmentData = equipmentData;
    }

    @NotNull
    public Equipment equipment() {
        return this.equipment;
    }

    /**
     * >= 1.21.2  NonNull
     * < 1.21.2 Null
     *
     * @return equipment data
     */
    @Nullable
    public EquipmentData equipmentData() {
        return this.equipmentData;
    }

    @NotNull
    public Tristate clientBoundModel() {
        return clientBoundModel;
    }
}
