package net.momirealms.craftengine.core.item.setting;

import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

public class EquipmentData {
    @NotNull
    private EquipmentSlot slot;
    @Nullable
    private Key assetId;
    private boolean dispensable;
    private boolean swappable;
    private boolean damageOnHurt;
    // 1.21.5+
    private boolean equipOnInteract;
    @Nullable
    private Key cameraOverlay;

    public EquipmentData(@NotNull EquipmentSlot slot,
                         @Nullable Key assetId,
                         boolean dispensable,
                         boolean swappable,
                         boolean damageOnHurt,
                         boolean equipOnInteract,
                         @Nullable Key cameraOverlay) {
        this.slot = slot;
        this.assetId = assetId;
        this.dispensable = dispensable;
        this.swappable = swappable;
        this.damageOnHurt = damageOnHurt;
        this.equipOnInteract = equipOnInteract;
        this.cameraOverlay = cameraOverlay;
    }

    public static EquipmentData fromMap(@NotNull final Map<String, Object> data) {
        String slot = (String) data.get("slot");
        if (slot == null) {
            throw new IllegalArgumentException("slot cannot be null");
        }
        EquipmentSlot slotEnum = EquipmentSlot.valueOf(slot.toUpperCase(Locale.ENGLISH).replace("_", ""));
        EquipmentData.Builder builder = EquipmentData.builder().slot(slotEnum);
        if (data.containsKey("asset-id")) {
            builder.assetId(Key.of(data.get("asset-id").toString()));
        }
        if (data.containsKey("camera-overlay")) {
            builder.cameraOverlay(Key.of(data.get("camera-overlay").toString()));
        }
        if (data.containsKey("dispensable")) {
            builder.dispensable(ResourceConfigUtils.getAsBoolean(data.get("dispensable"), "dispensable"));
        }
        if (data.containsKey("swappable")) {
            builder.swappable(ResourceConfigUtils.getAsBoolean(data.get("swappable"), "swappable"));
        }
        if (data.containsKey("equip-on-interact")) {
            builder.equipOnInteract(ResourceConfigUtils.getAsBoolean(data.get("equip-on-interact"), "equip-on-interact"));
        }
        if (data.containsKey("damage-on-hurt")) {
            builder.damageOnHurt(ResourceConfigUtils.getAsBoolean(data.get("damage-on-hurt"), "damage-on-hurt"));
        }
        return builder.build();
    }

    public EquipmentSlot slot() {
        return slot;
    }

    @Nullable
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

    @Nullable
    public Key cameraOverlay() {
        return cameraOverlay;
    }

    public void setSlot(@NotNull EquipmentSlot slot) {
        this.slot = slot;
    }

    public void setAssetId(@Nullable Key assetId) {
        this.assetId = assetId;
    }

    public void setDispensable(boolean dispensable) {
        this.dispensable = dispensable;
    }

    public void setSwappable(boolean swappable) {
        this.swappable = swappable;
    }

    public void setDamageOnHurt(boolean damageOnHurt) {
        this.damageOnHurt = damageOnHurt;
    }

    public void setEquipOnInteract(boolean equipOnInteract) {
        this.equipOnInteract = equipOnInteract;
    }

    public void setCameraOverlay(@Nullable Key cameraOverlay) {
        this.cameraOverlay = cameraOverlay;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("slot", this.slot.toString().toLowerCase(Locale.ENGLISH));
        if (this.assetId != null) {
            if (VersionHelper.isOrAbove1_21_4()) {
                tag.putString("asset_id", this.assetId.toString());
            } else {
                tag.putString("model", this.assetId.toString());
            }
        }
        tag.putBoolean("dispensable", this.dispensable);
        tag.putBoolean("swappable", this.swappable);
        tag.putBoolean("damage_on_hurt", this.damageOnHurt);
        if (VersionHelper.isOrAbove1_21_5()) {
            tag.putBoolean("equip_on_interact", this.equipOnInteract);
        }
        if (this.cameraOverlay != null) {
            tag.putString("camera_overlay", this.cameraOverlay.toString());
        }
        return tag;
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
        private boolean equipOnInteract = false;
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
