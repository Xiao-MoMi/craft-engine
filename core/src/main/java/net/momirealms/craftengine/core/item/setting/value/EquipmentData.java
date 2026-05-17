package net.momirealms.craftengine.core.item.setting.value;

import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

// todo 自定义装备声音
public final class EquipmentData {
    @NotNull
    private EquipmentSlot slot;
    @Nullable
    private Key assetId;
    private boolean dispensable;
    private boolean swappable;
    private boolean damageOnHurt;
    // 1.21.5+
    private boolean equipOnInteract;
    // 1.21.6+
    private boolean canBeSheared;
    @Nullable
    private Key cameraOverlay;

    public EquipmentData(@NotNull EquipmentSlot slot,
                         @Nullable Key assetId,
                         boolean dispensable,
                         boolean swappable,
                         boolean damageOnHurt,
                         boolean equipOnInteract,
                         boolean canBeSheared,
                         @Nullable Key cameraOverlay) {
        this.slot = slot;
        this.assetId = assetId;
        this.dispensable = dispensable;
        this.swappable = swappable;
        this.damageOnHurt = damageOnHurt;
        this.equipOnInteract = equipOnInteract;
        this.canBeSheared = canBeSheared;
        this.cameraOverlay = cameraOverlay;
    }

    private static final String[] ASSET_ID = new String[] {"asset_id", "asset-id"};
    private static final String[] CAMERA_OVERLAY = new String[] {"camera_overlay", "camera-overlay"};
    private static final String[] EQUIP_ON_INTERACT = new String[] {"equip_on_interact", "equip-on-interact"};
    private static final String[] DAMAGE_ON_HURT = new String[] {"damage_on_hurt", "damage-on-hurt"};
    private static final String[] CAN_BE_SHEARED = new String[] {"can_be_sheared", "can-be-sheared"};

    public static EquipmentData fromConfig(@NotNull final ConfigSection section) {
        EquipmentSlot slot = section.getNonNullEnum("slot", EquipmentSlot.class);
        Key assetId = section.getIdentifier(ASSET_ID);
        Key cameraOverlay = section.getIdentifier(CAMERA_OVERLAY);
        boolean dispensable = section.getBoolean("dispensable", true);
        boolean swappable = section.getBoolean("swappable", true);
        boolean equipOnInteract = section.getBoolean(EQUIP_ON_INTERACT);
        boolean damageOnHurt = section.getBoolean(DAMAGE_ON_HURT, true);
        boolean canBeSheared = section.getBoolean(CAN_BE_SHEARED);
        return new EquipmentData(slot, assetId, dispensable, swappable, damageOnHurt, equipOnInteract, canBeSheared, cameraOverlay);
    }

    public EquipmentSlot slot() {
        return this.slot;
    }

    @Nullable
    public Key assetId() {
        return this.assetId;
    }

    public boolean dispensable() {
        return this.dispensable;
    }

    public boolean swappable() {
        return this.swappable;
    }

    public boolean damageOnHurt() {
        return this.damageOnHurt;
    }

    public boolean equipOnInteract() {
        return this.equipOnInteract;
    }

    public boolean canBeSheared() {
        return this.canBeSheared;
    }

    @Nullable
    public Key cameraOverlay() {
        return this.cameraOverlay;
    }

    public void setSlot(@NotNull EquipmentSlot slot) {
        this.slot = slot;
    }

    public void setAssetId(@Nullable Key assetId) {
        this.assetId = assetId;
    }

    public void setCanBeSheared(boolean canBeSheared) {
        this.canBeSheared = canBeSheared;
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
        tag.putString("slot", this.slot.toString().toLowerCase(Locale.ROOT));
        if (this.assetId != null) {
            if (VersionHelper.isOrAbove1_21_4) {
                tag.putString("asset_id", this.assetId.asString());
            } else {
                tag.putString("model", this.assetId.asString());
            }
        }
        tag.putBoolean("dispensable", this.dispensable);
        tag.putBoolean("swappable", this.swappable);
        tag.putBoolean("damage_on_hurt", this.damageOnHurt);
        if (VersionHelper.isOrAbove1_21_5) {
            tag.putBoolean("equip_on_interact", this.equipOnInteract);
            if (VersionHelper.isOrAbove1_21_6) {
                tag.putBoolean("can_be_sheared", this.canBeSheared);
            }
        }
        if (this.cameraOverlay != null) {
            tag.putString("camera_overlay", this.cameraOverlay.asString());
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
        private boolean canBeSheared = false;
        private Key cameraOverlay;

        public Builder() {
        }

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

        public Builder canBeSheared(boolean canBeSheared) {
            this.canBeSheared = canBeSheared;
            return this;
        }

        public EquipmentData build() {
            return new EquipmentData(this.slot, this.assetId, this.dispensable, this.swappable, this.damageOnHurt, this.equipOnInteract, this.canBeSheared, this.cameraOverlay);
        }
    }
}
