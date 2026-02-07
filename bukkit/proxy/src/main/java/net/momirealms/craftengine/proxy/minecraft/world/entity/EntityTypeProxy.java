package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.EntityType")
public interface EntityTypeProxy {
    EntityTypeProxy INSTANCE = ASMProxyFactory.create(EntityTypeProxy.class);

    @FieldGetter(name = "dimensions")
    Object getDimensions(Object target);
}
