package net.momirealms.craftengine.bukkit.attribute.damage;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.attribute.AttributeEventListener;
import net.momirealms.craftengine.bukkit.attribute.BukkitAttributeManager;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.entity.BukkitEntityManager;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.attribute.*;
import net.momirealms.craftengine.core.attribute.damage.DamageEvent;
import net.momirealms.craftengine.core.attribute.damage.DamageSource;
import net.momirealms.craftengine.core.attribute.damage.EntityDamageContext;
import net.momirealms.craftengine.core.attribute.modifier.SlotAttributeModifierConfig;
import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.entity.LivingEntityHolder;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.setting.value.AttributeModifiers;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Cancellable;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.damage.CraftDamageSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.projectile.AbstractArrowProxy;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BukkitDamageEvent implements DamageEvent {
    private static final Key PLAYER_ATTACK = Key.minecraft("player_attack");

    private final EntityDamageEvent event;
    private final BukkitDamageSource source;
    private final Entity victim;
    private final AttributeGetter victimAttributes;
    private final AttributeGetter attackerAttributes;
    private final EntityDamageContext context;
    private final Item activeWeapon;
    private final float attackStrength;
    private final float shootForce;
    private Map<String, Double> damageParts;
    @Nullable
    private List<SlotAttributeModifierConfig> activeWeaponModifiers;

    public BukkitDamageEvent(EntityDamageEvent event) {
        this.event = event;
        this.source = new BukkitDamageSource(CraftDamageSourceProxy.INSTANCE.getHandle(event.getDamageSource()));
        org.bukkit.entity.Entity victim = event.getEntity();
        LivingEntityHolder victimHolder = BukkitEntityManager.instance().getEntityHolder(victim.getUniqueId());
        if (victimHolder == null) {
            this.victim = BukkitAdaptor.adapt(victim);
            this.victimAttributes = new NotTrackedHolder(this.victim);
        } else {
            this.victim = victimHolder.entity;
            this.victimAttributes = victimHolder.attributes();
        }
        this.attackerAttributes = this.causingEntityAttributes();
        this.attackStrength = this.resolveAttackStrength();
        this.shootForce = this.resolveShootForce();
        Item weapon = this.resolveActiveWeapon();
        this.activeWeapon = weapon == null || weapon.isEmpty() ? null : weapon;
        this.context = EntityDamageContext.of(this,
                ContextHolder.builder()
                        .withOptionalParameter(DirectContextParameters.ITEM, this.activeWeapon)
                        .withParameter(DirectContextParameters.EVENT, Cancellable.of(event::isCancelled, event::setCancelled))
        );
    }

    @Override
    public EntityDamageContext context() {
        return this.context;
    }

    @Override
    public double damage() {
        return this.event.getDamage();
    }

    @Override
    public double finalDamage() {
        return this.event.getFinalDamage();
    }

    @Override
    public void setDamage(double damage) {
        this.event.setDamage(damage);
        this.context.contexts().withParameter(DirectContextParameters.DAMAGE, damage);
    }

    @Override
    public DamageSource source() {
        return this.source;
    }

    @Override
    public Entity victim() {
        return this.victim;
    }

    @Override
    public boolean isSweepAttack() {
        return this.event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK;
    }

    @Override
    public float attackStrength() {
        return this.attackStrength;
    }

    @Override
    public float shootForce() {
        return this.shootForce;
    }

    @Override
    public void recordDamagePart(String id, double amount) {
        this.damageParts().put(id, amount);
        this.context.contexts().withParameter(ContextKey.direct("damage_" + id), amount);
    }

    @Override
    public void initFinalDamage() {
        this.context.contexts().withParameter(DirectContextParameters.FINAL_DAMAGE, this.finalDamage());
    }

    @Override
    public Map<String, Double> damageParts() {
        if (this.damageParts == null) {
            this.damageParts = new LinkedHashMap<>();
        }
        return this.damageParts;
    }

    private float resolveAttackStrength() {
        if (!PLAYER_ATTACK.equals(this.source.type())) return 1.0F;
        if (!VersionHelper.hasPaperPatch) return 1.0F;
        if (!(this.source.causingEntity() instanceof BukkitServerPlayer player)) return 0.0F;
        return player.capturedAttackStrength();
    }

    private float resolveShootForce() {
        BukkitEntity directEntity = this.source.directEntity();
        if (directEntity == null) return 1.0F;
        Float force = directEntity.platformEntity().getPersistentDataContainer().get(
                AttributeEventListener.PROJECTILE_SHOOT_FORCE,
                PersistentDataType.FLOAT
        );
        if (force == null || !Float.isFinite(force)) return 1.0F;
        return Math.clamp(force, 0.0F, 1.0F);
    }

    public AttributeGetter causingEntityAttributes() {
        BukkitEntity directEntity = this.source.directEntity();
        if (directEntity != null) {
            EntityAttributesSnapshot snapshot = directEntity.getCustomData(EntityAttributesSnapshot.PROJECTILE_DATA_KEY);
            if (snapshot != null) {
                return snapshot;
            }
        }
        BukkitEntity entity = this.source.causingEntity();
        if (entity == null) {
            return EmptyAttributeHolder.INSTANCE;
        }
        LivingEntityHolder holder = BukkitEntityManager.instance().getEntityHolder(entity.uuid());
        AttributeGetter attributes = holder == null ? null : holder.attributes();
        return attributes == null ? new NotTrackedHolder(entity) : attributes;
    }

    @Override
    public double getAttributeValue(AttributeSide side, Attribute attribute) {
        if (attribute.derived() != null) {
            return attribute.derive(a -> getAttributeValue(side, a));
        }
        if (side == AttributeSide.ATTACKER) {
            return this.attackerAttributes.getAttributeValue(attribute) + weaponAttributeValue(attribute);
        } else {
            return this.victimAttributes.getAttributeValue(attribute);
        }
    }

    private double weaponAttributeValue(Attribute attribute) {
        Item weapon = this.activeWeapon;
        if (weapon == null || weapon.isEmpty()) return 0;
        if (this.activeWeaponModifiers == null) {
            this.activeWeaponModifiers = BukkitAttributeManager.instance().getItemAttributeModifiers(weapon);
        }
        return AttributeModifiers.weaponValue(this.activeWeaponModifiers, attribute, this.context);
    }

    @Nullable
    @Override
    public Item activeWeapon() {
        return this.activeWeapon;
    }

    @Nullable
    private Item resolveActiveWeapon() {
        // 近战等直接伤害：攻击者主手物品
        if (this.source.isDirect()) {
            if (this.source.causingEntity() instanceof LivingEntity living) {
                return living.getItemInHand(InteractionHand.MAIN_HAND);
            }
            return null;
        }
        // 弹射物的武器
        Object direct = this.source.directNmsEntity();
        if (AbstractArrowProxy.CLASS.isInstance(direct)) {
            if (VersionHelper.isOrAbove1_21) {
                Object weaponStack = AbstractArrowProxy.INSTANCE.getWeaponItem(direct);
                if (weaponStack != null) {
                    return ItemStackUtils.wrap(weaponStack);
                }
            } else {
                // TODO 暂不支持
            }
        }
        return null;
    }
}
