package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class EquipmentData {
    @NotNull
    private final EquipmentSlot slot;
    @Nullable
    private final Key assetId;
    private final boolean dispensable;
    private final boolean swappable;
    private final boolean damageOnHurt;
    // 1.21.5+
    private final boolean equipOnInteract;
    @Nullable
    private final Key cameraOverlay;

    public EquipmentData(@NotNull EquipmentSlot slot, @Nullable Key assetId, boolean dispensable, boolean swappable, boolean damageOnHurt, boolean equipOnInteract, @Nullable Key cameraOverlay) {
        this.slot = slot;
        this.assetId = assetId;
        this.dispensable = dispensable;
        this.swappable = swappable;
        this.damageOnHurt = damageOnHurt;
        this.equipOnInteract = equipOnInteract;
        this.cameraOverlay = cameraOverlay;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public Key assetId() {
        return assetId;
    }

    public boolean dispensable() {
        return dispensable;
    }

    public boolean swappable() {
        return swappable;
    }

    public boolean damageOnHurt() {
        return damageOnHurt;
    }

    public boolean equipOnInteract() {
        return equipOnInteract;
    }

    public Key cameraOverlay() {
        return cameraOverlay;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("slot", this.slot.toString());
        if (this.assetId != null) {
            map.put("asset_id", this.assetId.toString());
        }
        map.put("dispensable", this.dispensable);
        map.put("swappable", this.swappable);
        map.put("damage_on_hurt", this.damageOnHurt);
        if (VersionHelper.isVersionNewerThan1_21_5()) {
            map.put("equip_on_interact", this.equipOnInteract);
        }
        if (this.cameraOverlay != null) {
            map.put("camera_overlay", this.cameraOverlay.toString());
        }
        return map;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EquipmentSlot slot = EquipmentSlot.HEAD;
        private Key assetId;
        private boolean dispensable = true;
        private boolean swappable = true;
        private boolean damageOnHurt = true;
        // 1.21.5+
        private boolean equipOnInteract = true;
        private Key cameraOverlay;

        public Builder() {}

        public Builder slot(EquipmentSlot slot) {
            this.slot = slot;
            return this;
        }

        public Builder assetId(Key assetId) {
            this.assetId = assetId;
            return this;
        }

        public Builder dispensable(boolean dispensable) {
            this.dispensable = dispensable;
            return this;
        }

        public Builder swappable(boolean swappable) {
            this.swappable = swappable;
            return this;
        }

        public Builder damageOnHurt(boolean damageOnHurt) {
            this.damageOnHurt = damageOnHurt;
            return this;
        }

        public Builder equipOnInteract(boolean equipOnInteract) {
            this.equipOnInteract = equipOnInteract;
            return this;
        }

        public Builder cameraOverlay(Key cameraOverlay) {
            this.cameraOverlay = cameraOverlay;
            return this;
        }

        public EquipmentData build() {
            return new EquipmentData(slot, assetId, dispensable, swappable, damageOnHurt, equipOnInteract, cameraOverlay);
        }
    }
}
