package net.momirealms.craftengine.bukkit.entity;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.momirealms.craftengine.bukkit.attribute.BukkitVanillaAttributeInstance;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeInstance;
import net.momirealms.craftengine.core.entity.EquipmentSlot;
import net.momirealms.craftengine.core.entity.effect.PotionEffectSnapshot;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.effect.MobEffectInstanceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EquipmentSlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;

public class BukkitLivingEntity extends BukkitEntity implements net.momirealms.craftengine.core.entity.LivingEntity {
    private Object2ObjectOpenHashMap<Key, BukkitVanillaAttributeInstance> vanillaAttributes;

    protected BukkitLivingEntity(WeakReference<Object> entity) {
        super(entity);
    }

    public BukkitLivingEntity(Object entity) {
        super(entity);
    }

    @Nullable
    @Override
    public VanillaAttributeInstance getVanillaAttribute(Key attribute) {
        Object instance;
        if (VersionHelper.isOrAbove1_20_5) {
            Object holder = RegistryUtils.getHolderById(BuiltInRegistriesProxy.ATTRIBUTE, KeyUtils.toIdentifier(attribute));
            if (holder == null) return null;
            instance = LivingEntityProxy.INSTANCE.getAttribute(minecraftEntity(), holder);
        } else {
            Object attributeObject = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.ATTRIBUTE, KeyUtils.toIdentifier(attribute));
            if (attributeObject == null) return null;
            instance = LivingEntityProxy.INSTANCE.getAttribute$legacy(minecraftEntity(), attributeObject);
        }
        if (instance == null) return null;
        return new BukkitVanillaAttributeInstance(instance);
    }

    @Override
    public double health() {
        return platformEntity().getHealth();
    }

    @Override
    public void setHealth(double amount) {
        platformEntity().setHealth(amount);
    }

    @Override
    public double maxHealth() {
        if (VersionHelper.isOrAbove1_21) {
            return Objects.requireNonNull(platformEntity().getAttribute(Attribute.MAX_HEALTH)).getValue();
        } else {
            return LegacyAttributeUtils.getMaxHealth(platformEntity());
        }
    }

    @Override
    public double luck() {
        if (VersionHelper.isOrAbove1_21_3) {
            return Optional.ofNullable(platformEntity().getAttribute(Attribute.LUCK)).map(AttributeInstance::getValue).orElse(1d);
        } else {
            return LegacyAttributeUtils.getLuck(platformEntity());
        }
    }

    @Override
    public void damage(double amount, Key damageType, @Nullable net.momirealms.craftengine.core.entity.Entity causingEntity) {
        @SuppressWarnings("deprecation")
        DamageType type = Registry.DAMAGE_TYPE.get(KeyUtils.toNamespacedKey(damageType));
        DamageSource source = DamageSource.builder(type != null ? type : DamageType.GENERIC)
                .withCausingEntity(causingEntity != null ? (Entity) causingEntity.platformEntity() : this.platformEntity())
                .withDirectEntity(this.platformEntity())
                .withDamageLocation(this.platformEntity().getLocation())
                .build();
        this.platformEntity().damage(amount, source);
    }

    @Override
    public void addPotionEffect(Key potionEffectType, int duration, int amplifier, boolean ambient, boolean particles, boolean showIcon) {
        if (VersionHelper.isOrAbove1_20_5) {
            Object holder = RegistryUtils.getHolderById(BuiltInRegistriesProxy.MOB_EFFECT, KeyUtils.toIdentifier(potionEffectType));
            if (holder != null) {
                Object mobEffect = MobEffectInstanceProxy.INSTANCE.newInstance(holder, duration, amplifier, ambient, particles, showIcon);
                LivingEntityProxy.INSTANCE.addEffect(minecraftEntity(), mobEffect);
            }
        } else {
            Object mobEffect = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.MOB_EFFECT, KeyUtils.toIdentifier(potionEffectType));
            if (mobEffect != null) {
                LivingEntityProxy.INSTANCE.addEffect(minecraftEntity(), MobEffectInstanceProxy.INSTANCE.newInstance$legacy(mobEffect, duration, amplifier, ambient, particles, showIcon));
            }
        }
    }

    @Nullable
    @Override
    public PotionEffectSnapshot getPotionEffect(Key potionEffectType) {
        Object effect;
        if (VersionHelper.isOrAbove1_20_5) {
            Object holder = RegistryUtils.getHolderById(BuiltInRegistriesProxy.MOB_EFFECT, KeyUtils.toIdentifier(potionEffectType));
            if (holder == null) return null;
            effect = LivingEntityProxy.INSTANCE.getEffect(minecraftEntity(), holder);
        } else {
            Object mobEffect = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.MOB_EFFECT, KeyUtils.toIdentifier(potionEffectType));
            if (mobEffect == null) return null;
            effect = LivingEntityProxy.INSTANCE.getEffect$legacy(minecraftEntity(), mobEffect);
        }
        if (effect == null) return null;
        return new PotionEffectSnapshot(
                potionEffectType,
                MobEffectInstanceProxy.INSTANCE.getDuration(effect),
                MobEffectInstanceProxy.INSTANCE.getAmplifier(effect),
                MobEffectInstanceProxy.INSTANCE.isAmbient(effect),
                MobEffectInstanceProxy.INSTANCE.isVisible(effect),
                MobEffectInstanceProxy.INSTANCE.showIcon(effect)
        );
    }

    @Override
    public void removePotionEffect(Key potionEffectType) {
        if (VersionHelper.isOrAbove1_20_5) {
            Object holder = RegistryUtils.getHolderById(BuiltInRegistriesProxy.MOB_EFFECT, KeyUtils.toIdentifier(potionEffectType));
            if (holder != null) {
                LivingEntityProxy.INSTANCE.removeEffect(minecraftEntity(), holder);
            }
        } else {
            Object mobEffect = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.MOB_EFFECT, KeyUtils.toIdentifier(potionEffectType));
            if (mobEffect != null) {
                LivingEntityProxy.INSTANCE.removeEffect$legacy(minecraftEntity(), mobEffect);
            }
        }
    }

    @Override
    public void clearPotionEffects() {
        LivingEntityProxy.INSTANCE.removeAllEffects(minecraftEntity());
    }

    @NotNull
    @Override
    public BukkitItem getItemInHand(InteractionHand hand) {
        return ItemStackUtils.wrap(LivingEntityProxy.INSTANCE.getItemBySlot(minecraftEntity(), hand == InteractionHand.MAIN_HAND ? EquipmentSlotProxy.MAINHAND : EquipmentSlotProxy.OFFHAND));
    }

    @Override
    public void setItemInHand(InteractionHand hand, Item item) {
        LivingEntityProxy.INSTANCE.setItemSlot(minecraftEntity(), hand == InteractionHand.MAIN_HAND ? EquipmentSlotProxy.MAINHAND : EquipmentSlotProxy.OFFHAND, item.minecraftItem());
    }

    @NotNull
    @Override
    public BukkitItem getItemByEquipmentSlot(EquipmentSlot slot) {
        Object nmsSlot = EquipmentSlotUtils.toNMSEquipmentSlot(slot);
        if (nmsSlot == null) return (BukkitItem) BukkitItemManager.instance().emptyItem();
        return ItemStackUtils.wrap(LivingEntityProxy.INSTANCE.getItemBySlot(minecraftEntity(), nmsSlot));
    }

    @Override
    public LivingEntity platformEntity() {
        return (LivingEntity) super.platformEntity();
    }

    @Override
    public boolean isSneaking() {
        return EntityProxy.INSTANCE.isShiftKeyDown(minecraftEntity());
    }

    @Override
    public boolean isSwimming() {
        return platformEntity().isSwimming();
    }

    @Override
    public boolean isClimbing() {
        return platformEntity().isClimbing();
    }

    @Override
    public boolean isGliding() {
        return platformEntity().isGliding();
    }
}
