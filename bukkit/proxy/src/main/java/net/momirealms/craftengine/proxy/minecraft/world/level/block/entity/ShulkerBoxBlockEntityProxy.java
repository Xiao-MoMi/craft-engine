package net.momirealms.craftengine.proxy.minecraft.world.level.block.entity;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity")
public interface ShulkerBoxBlockEntityProxy {
    ShulkerBoxBlockEntityProxy INSTANCE = ASMProxyFactory.create(ShulkerBoxBlockEntityProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity");

    @MethodInvoker(name = "getAnimationStatus")
    Object getAnimationStatus(Object target);

    @MethodInvoker(name = "getProgress")
    float getProgress(Object target, float partialTick);

    @ReflectionProxy(name = "net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity$AnimationStatus")
    interface AnimationStatusProxy {
        AnimationStatusProxy INSTANCE = ASMProxyFactory.create(AnimationStatusProxy.class);
        Object OPENING = INSTANCE.getOpening();
        Object CLOSING = INSTANCE.getClosing();

        @FieldGetter(name = "OPENING", isStatic = true)
        Object getOpening();

        @FieldGetter(name = "CLOSING", isStatic = true)
        Object getClosing();
    }
}
