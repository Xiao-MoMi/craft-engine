package net.momirealms.craftengine.proxy.minecraft.world.entity;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.function.Predicate;

@ReflectionProxy(name = "net.minecraft.world.entity.EntitySelector")
public interface EntitySelectorProxy {
    EntitySelectorProxy INSTANCE = ASMProxyFactory.create(EntitySelectorProxy.class);
    Predicate<Object> NO_SPECTATORS = INSTANCE.getNoSpectators();

    @FieldGetter(name = "NO_SPECTATORS", isStatic = true)
    Predicate<Object> getNoSpectators();
}
