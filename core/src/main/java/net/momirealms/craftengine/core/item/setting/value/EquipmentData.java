package net.momirealms.craftengine.core.item.setting.value;

import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Pair;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

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
    @Nullable
    private Pair<Key, @Nullable Float> equipSound;
    @Nullable
    private Pair<Key, @Nullable Float> shearingSound;

    public EquipmentData(@NotNull EquipmentSlot slot,
                         @Nullable Key assetId,
                         boolean dispensable,
                         boolean swappable,
                         boolean damageOnHurt,
                         boolean equipOnInteract,
                         boolean canBeSheared,
                         @Nullable Key cameraOverlay,
                         @Nullable Pair<Key, Float> equipSound,
                         @Nullable Pair<Key, Float> shearingSound) {
        this.slot = slot;
        this.assetId = assetId;
        this.dispensable = dispensable;
        this.swappable = swappable;
        this.damageOnHurt = damageOnHurt;
        this.equipOnInteract = equipOnInteract;
        this.canBeSheared = canBeSheared;
        this.cameraOverlay = cameraOverlay;
        this.equipSound = equipSound;
        this.shearingSound = shearingSound;
    }

    private static final String[] ASSET_ID = ConfigKeys.of("asset_id");
    private static final String[] CAMERA_OVERLAY = ConfigKeys.of("camera_overlay");
    private static final String[] EQUIP_ON_INTERACT = ConfigKeys.of("equip_on_interact");
    private static final String[] DAMAGE_ON_HURT = ConfigKeys.of("damage_on_hurt");
    private static final String[] CAN_BE_SHEARED = ConfigKeys.of("can_be_sheared");
    private static final String[] EQUIP_SOUND = ConfigKeys.of("equip_sound");
    private static final String[] SHEARING_SOUND = ConfigKeys.of("shearing_sound");
    private static final String[] SOUND_ID = ConfigKeys.of("sound_id");

    public static EquipmentData fromConfig(@NotNull final ConfigSection section) {
        EquipmentSlot slot = section.getNonNullEnum("slot", EquipmentSlot.class, EquipmentSlot::byId);
        Key assetId = section.getIdentifier(ASSET_ID);
        Key cameraOverlay = section.getIdentifier(CAMERA_OVERLAY);
        boolean dispensable = section.getBoolean("dispensable", true);
        boolean swappable = section.getBoolean("swappable", true);
        boolean equipOnInteract = section.getBoolean(EQUIP_ON_INTERACT);
        boolean damageOnHurt = section.getBoolean(DAMAGE_ON_HURT, true);
        boolean canBeSheared = section.getBoolean(CAN_BE_SHEARED);
        Pair<Key, Float> equipSound = section.getValue(EQUIP_SOUND, EquipmentData::parseSound);
        Pair<Key, Float> shearingSound = section.getValue(SHEARING_SOUND, EquipmentData::parseSound);
        return new EquipmentData(slot, assetId, dispensable, swappable, damageOnHurt, equipOnInteract, canBeSheared, cameraOverlay, equipSound, shearingSound);
    }

    private static Pair<Key, @Nullable Float> parseSound(ConfigValue value) {
        if (value.is(Map.class)) {
            ConfigSection configSection = value.getAsSection();
            Key id = configSection.getNonNullIdentifier(SOUND_ID);
            Float range = configSection.containsKey("range") ? configSection.getFloat("range") : null;
            return Pair.of(id, range);
        } else {
            return Pair.of(value.getAsIdentifier(), null);
        }
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

    @Nullable
    public Pair<Key, @Nullable Float> equipSound() {
        return this.equipSound;
    }

    @Nullable
    public Pair<Key, @Nullable Float> shearingSound() {
        return this.shearingSound;
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

    public void setEquipSound(@Nullable Pair<Key, @Nullable Float> equipSound) {
        this.equipSound = equipSound;
    }

    public void setShearingSound(@Nullable Pair<Key, @Nullable Float> shearingSound) {
        this.shearingSound = shearingSound;
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
        if (this.equipSound != null) {
            Key id = this.equipSound.left();
            Float range = this.equipSound.right();
            if (range == null) {
                tag.putString("equip_sound", id.asString());
            } else {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putString("sound_id", id.asString());
                compoundTag.putFloat("range", range);
                tag.put("equip_sound", compoundTag);
            }
        }
        if (this.shearingSound != null) {
            Key id = this.shearingSound.left();
            Float range = this.shearingSound.right();
            if (range == null) {
                tag.putString("shearing_sound", id.asString());
            } else {
                CompoundTag compoundTag = new CompoundTag();
                compoundTag.putString("sound_id", id.asString());
                compoundTag.putFloat("range", range);
                tag.put("shearing_sound", compoundTag);
            }
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
        private Pair<Key, Float> equipSound;
        private Pair<Key, Float> shearingSound;

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

        public Builder equipSound(Pair<Key, Float> equipSound) {
            this.equipSound = equipSound;
            return this;
        }

        public Builder shearingSound(Pair<Key, Float> shearingSound) {
            this.shearingSound = shearingSound;
            return this;
        }

        public EquipmentData build() {
            return new EquipmentData(this.slot, this.assetId, this.dispensable, this.swappable, this.damageOnHurt, this.equipOnInteract, this.canBeSheared, this.cameraOverlay, this.equipSound, this.shearingSound);
        }
    }
}
