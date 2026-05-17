package net.momirealms.craftengine.proxy.minecraft.network.syncher;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.syncher.EntityDataSerializers")
public interface EntityDataSerializersProxy {
    EntityDataSerializersProxy INSTANCE = ASMProxyFactory.create(EntityDataSerializersProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.syncher.EntityDataSerializers");
    Object BYTE = INSTANCE.getByte();
    Object INT = INSTANCE.getInt();
    Object LONG = INSTANCE.getLong();
    Object FLOAT = INSTANCE.getFloat();
    Object STRING = INSTANCE.getString();
    Object COMPONENT = INSTANCE.getComponent();
    Object OPTIONAL_COMPONENT = INSTANCE.getOptionalComponent();
    Object ITEM_STACK = INSTANCE.getItemStack();
    Object BLOCK_STATE = INSTANCE.getBlockState();
    Object OPTIONAL_BLOCK_STATE = INSTANCE.getOptionalBlockState();
    Object BOOLEAN = INSTANCE.getBoolean();
    Object PARTICLE = INSTANCE.getParticle();
    Object PARTICLES = INSTANCE.getParticles();
    Object ROTATIONS = INSTANCE.getRotations();
    Object BLOCK_POS = INSTANCE.getBlockPos();
    Object OPTIONAL_BLOCK_POS = INSTANCE.getOptionalBlockPos();
    Object DIRECTION = INSTANCE.getDirection();
    Object OPTIONAL_UUID = INSTANCE.getOptionalUUID();
    Object OPTIONAL_LIVING_ENTITY_REFERENCE = INSTANCE.getOptionalLivingEntityReference();
    Object OPTIONAL_GLOBAL_POS = INSTANCE.getOptionalGlobalPos();
    Object COMPOUND_TAG = INSTANCE.getCompoundTag();
    Object VILLAGER_DATA = INSTANCE.getVillagerData();
    Object OPTIONAL_UNSIGNED_INT = INSTANCE.getOptionalUnsignedInt();
    Object POSE = INSTANCE.getPose();
    Object CAT_VARIANT = INSTANCE.getCatVariant();
    Object WOLF_VARIANT = INSTANCE.getWolfVariant();
    Object FROG_VARIANT = INSTANCE.getFrogVariant();
    Object PAINTING_VARIANT = INSTANCE.getPaintingVariant();
    Object ARMADILLO_STATE = INSTANCE.getArmadilloState();
    Object SNIFFER_STATE = INSTANCE.getSnifferState();
    Object VECTOR3 = INSTANCE.getVector3();
    Object QUATERNION = INSTANCE.getQuaternion();
    Object RESOLVABLE_PROFILE = INSTANCE.getResolvableProfile();
    Object HUMANOID_ARM = INSTANCE.getHumanoidArm();

    @FieldGetter(name = "BYTE", isStatic = true)
    Object getByte();

    @FieldGetter(name = "INT", isStatic = true)
    Object getInt();

    @FieldGetter(name = "LONG", isStatic = true)
    Object getLong();

    @FieldGetter(name = "FLOAT", isStatic = true)
    Object getFloat();

    @FieldGetter(name = "STRING", isStatic = true)
    Object getString();

    @FieldGetter(name = "COMPONENT", isStatic = true)
    Object getComponent();

    @FieldGetter(name = "OPTIONAL_COMPONENT", isStatic = true)
    Object getOptionalComponent();

    @FieldGetter(name = "ITEM_STACK", isStatic = true)
    Object getItemStack();

    @FieldGetter(name = "BLOCK_STATE", isStatic = true)
    Object getBlockState();

    @FieldGetter(name = "OPTIONAL_BLOCK_STATE", isStatic = true)
    Object getOptionalBlockState();

    @FieldGetter(name = "BOOLEAN", isStatic = true)
    Object getBoolean();

    @FieldGetter(name = "PARTICLE", isStatic = true)
    Object getParticle();

    @FieldGetter(name = "PARTICLES", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getParticles() {
        return null;
    }

    @FieldGetter(name = "ROTATIONS", isStatic = true)
    Object getRotations();

    @FieldGetter(name = "BLOCK_POS", isStatic = true)
    Object getBlockPos();

    @FieldGetter(name = "OPTIONAL_BLOCK_POS", isStatic = true)
    Object getOptionalBlockPos();

    @FieldGetter(name = "DIRECTION", isStatic = true)
    Object getDirection();

    @FieldGetter(name = "OPTIONAL_UUID", isStatic = true, activeIf = "max_version=1.21.4")
    default Object getOptionalUUID() {
        return null;
    }

    @FieldGetter(name = "OPTIONAL_LIVING_ENTITY_REFERENCE", isStatic = true, activeIf = "min_version=1.21.5")
    default Object getOptionalLivingEntityReference() {
        return null;
    }

    @FieldGetter(name = "OPTIONAL_GLOBAL_POS", isStatic = true)
    Object getOptionalGlobalPos();

    @FieldGetter(name = "COMPOUND_TAG", isStatic = true, activeIf = "max_version=1.21.8")
    default Object getCompoundTag() {
        return null;
    }

    @FieldGetter(name = "VILLAGER_DATA", isStatic = true)
    Object getVillagerData();

    @FieldGetter(name = "OPTIONAL_UNSIGNED_INT", isStatic = true)
    Object getOptionalUnsignedInt();

    @FieldGetter(name = "POSE", isStatic = true)
    Object getPose();

    @FieldGetter(name = "CAT_VARIANT", isStatic = true)
    Object getCatVariant();

    @FieldGetter(name = "WOLF_VARIANT", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getWolfVariant() {
        return null;
    }

    @FieldGetter(name = "FROG_VARIANT", isStatic = true)
    Object getFrogVariant();

    @FieldGetter(name = "PAINTING_VARIANT", isStatic = true)
    Object getPaintingVariant();

    @FieldGetter(name = "ARMADILLO_STATE", isStatic = true, activeIf = "min_version=1.20.5")
    default Object getArmadilloState() {
        return null;
    }

    @FieldGetter(name = "SNIFFER_STATE", isStatic = true)
    Object getSnifferState();

    @FieldGetter(name = "VECTOR3", isStatic = true)
    Object getVector3();

    @FieldGetter(name = "QUATERNION", isStatic = true)
    Object getQuaternion();

    @FieldGetter(name = "RESOLVABLE_PROFILE", isStatic = true, activeIf = "min_version=1.21.9")
    default Object getResolvableProfile() {
        return null;
    }

    @FieldGetter(name = "HUMANOID_ARM", isStatic = true, activeIf = "min_version=1.21.11")
    default Object getHumanoidArm() {
        return null;
    }

}
