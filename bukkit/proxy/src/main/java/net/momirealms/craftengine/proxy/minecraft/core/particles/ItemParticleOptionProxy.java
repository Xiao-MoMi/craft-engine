package net.momirealms.craftengine.proxy.minecraft.core.particles;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.core.particles.ItemParticleOption")
public interface ItemParticleOptionProxy extends ParticleOptionsProxy {
    ItemParticleOptionProxy INSTANCE = ASMProxyFactory.create(ItemParticleOptionProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.core.particles.ItemParticleOption");

    @ConstructorInvoker(activeIf = "max_version=1.21.11")
    Object newInstance$0(@Type(clazz = ParticleTypeProxy.class) Object type, @Type(clazz = ItemStackProxy.class) Object stack);

    @ConstructorInvoker(activeIf = "min_version=26.1")
    Object newInstance$1(@Type(clazz = ParticleTypeProxy.class) Object type, @Type(clazz = ItemStackTemplateProxy.class) Object template);

    @FieldGetter(name = "itemStack")
    Object getItemStack(Object target);
}
