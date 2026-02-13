package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.craftengine.proxy.minecraft.world.damagesource.DamageSourceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.bukkit.entity.Entity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@ReflectionProxy(name = "net.minecraft.world.entity.Entity")
public interface EntityProxy {
    EntityProxy INSTANCE = ASMProxyFactory.create(EntityProxy.class);
    AtomicInteger ENTITY_COUNTER = INSTANCE.getEntityCounter();

    @FieldGetter(name = "ENTITY_COUNTER")
    AtomicInteger getEntityCounter();

    @FieldGetter(name = "xo")
    double getXo(Object target);

    @FieldSetter(name = "xo")
    void setXo(Object target, double xo);

    @FieldGetter(name = "yo")
    double getYo(Object target);

    @FieldSetter(name = "yo")
    void setYo(Object target, double yo);

    @FieldGetter(name = "zo")
    double getZo(Object target);

    @FieldSetter(name = "zo")
    void setZo(Object target, double zo);

    @FieldGetter(name = "uuid")
    UUID getUUID(Object target);

    @FieldSetter(name = "uuid")
    void setUUID(Object target, UUID uuid);

    @FieldGetter(name = "position")
    Object getPosition(Object target);

    @FieldGetter(name = "entityData")
    Object getEntityData(Object target);

    @FieldSetter(name = "entityData")
    void setEntityData(Object target, Object entityData);

    @FieldGetter(name = "hurtMarked")
    boolean getHurtMarked(Object target);

    @FieldSetter(name = "hurtMarked")
    void setHurtMarked(Object target, boolean hurtMarked);

    @FieldGetter(name = {"trackedEntity", "tracker"})
    Object getTrackedEntity(Object target);

    @FieldSetter(name = {"trackedEntity", "tracker"})
    void setTrackedEntity(Object target, Object trackedEntity);

    @FieldGetter(name = "wasTouchingWater")
    boolean isWasTouchingWater(Object target);

    @FieldSetter(name = "wasTouchingWater")
    void setWasTouchingWater(Object target, boolean wasTouchingWater);

    @MethodInvoker(name = "getOnPos")
    Object getOnPos(Object target);

    @MethodInvoker(name = "getPassengerRidingPosition", activeIf = "min_version=1.20.2")
    Object getPassengerRidingPosition(Object target, @Type(clazz = EntityProxy.class) Object passenger);

    @MethodInvoker(name = "getMyRidingOffset", activeIf = "min_version=1.20.2 && max_version=1.20.4")
    float getMyRidingOffset(Object target, @Type(clazz = EntityProxy.class) Object vehicle);

    @MethodInvoker(name = "getMyRidingOffset", activeIf = "max_version=1.20.1")
    double getMyRidingOffset(Object target);

    @MethodInvoker(name = "getVehicleAttachmentPoint", activeIf = "min_version=1.20.5")
    Object getVehicleAttachmentPoint(Object target, @Type(clazz = EntityProxy.class) Object vehicle);

    @MethodInvoker(name = "getPassengersRidingOffset", activeIf = "max_version=1.20.1")
    double getPassengersRidingOffset(Object target);

    @MethodInvoker(name = "isIgnoringBlockTriggers")
    boolean isIgnoringBlockTriggers(Object target);

    @MethodInvoker(name = "isSpectator")
    boolean isSpectator(Object target);

    @MethodInvoker(name = "setDeltaMovement")
    void setDeltaMovement(Object target, double x, double y, double z);

    @MethodInvoker(name = "getDeltaMovement")
    Object getDeltaMovement(Object target);

    @MethodInvoker(name = "getSharedFlag")
    boolean getSharedFlag(Object target, int flag);

    @MethodInvoker(name = "damageSources")
    Object damageSources(Object target);

    @MethodInvoker(name = "causeFallDamage", activeIf = "max_version=1.21.4")
    boolean causeFallDamage(Object target, float fallDistance, float damageMultiplier, @Type(clazz = DamageSourceProxy.class) Object damageSource);

    @MethodInvoker(name = "causeFallDamage", activeIf = "min_version=1.21.5")
    boolean causeFallDamage(Object target, double fallDistance, float damageMultiplier, @Type(clazz = DamageSourceProxy.class) Object damageSource);

    @MethodInvoker(name = "getType")
    Object getType(Object target);

    @MethodInvoker(name = "getId")
    int getId(Object target);

    @MethodInvoker(name = "getBukkitEntity")
    Entity getBukkitEntity(Object target);

    @MethodInvoker(name = "isRemoved")
    boolean isRemoved(Object target);

    @FieldGetter(name = "blocksBuilding")
    boolean getBlocksBuilding(Object target);

    @MethodInvoker(name = "getBoundingBox")
    Object getBoundingBox(Object target);
}
