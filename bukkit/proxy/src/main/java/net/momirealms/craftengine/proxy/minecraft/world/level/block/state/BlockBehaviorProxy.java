package net.momirealms.craftengine.proxy.minecraft.world.level.block.state;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockBehaviour")
public interface BlockBehaviorProxy {
    BlockBehaviorProxy INSTANCE = ASMProxyFactory.create(BlockBehaviorProxy.class);

    @FieldGetter(name = "hasCollision")
    boolean hasCollision(Object target);

    @FieldSetter(name = "hasCollision")
    void setHasCollision(Object target, boolean hasCollision);

    @FieldGetter(name = "isRandomlyTicking")
    boolean isRandomlyTicking(Object target);

    @FieldSetter(name = "isRandomlyTicking")
    void setIsRandomlyTicking(Object target, boolean isRandomlyTicking);

    @FieldGetter(name = "explosionResistance")
    float getExplosionResistance(Object target);

    @FieldSetter(name = "explosionResistance")
    void setExplosionResistance(Object target, float resistance);

    @FieldGetter(name = "soundType")
    Object getSoundType(Object target);

    @FieldSetter(name = "soundType")
    void setSoundType(Object target, Object soundType);

    @FieldGetter(name = "friction")
    float getFriction(Object target);

    @FieldSetter(name = "friction")
    void setFriction(Object target, float friction);

    @FieldGetter(name = "speedFactor")
    float getSpeedFactor(Object target);

    @FieldSetter(name = "speedFactor")
    void setSpeedFactor(Object target, float speedFactor);

    @FieldGetter(name = "jumpFactor")
    float getJumpFactor(Object target);

    @FieldSetter(name = "jumpFactor")
    void setJumpFactor(Object target, float jumpFactor);

    @FieldGetter(name = "descriptionId", activeIf = ">=1.21.2")
    String getDescriptionId(Object target);

    @FieldSetter(name = "descriptionId", activeIf = ">=1.21.2")
    void setDescriptionId(Object target, String descriptionId);

    @ReflectionProxy(name = "net.minecraft.world.level.block.state.BlockBehaviour$Properties")
    interface PropertiesProxy {
        PropertiesProxy INSTANCE = ASMProxyFactory.create(PropertiesProxy.class);

        @MethodInvoker(name = "of", isStatic = true)
        Object of();

        @FieldGetter(name = "id", activeIf = "min_version=1.21.2")
        void setId(Object target, Object id);
    }
}