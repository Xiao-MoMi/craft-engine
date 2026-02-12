package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.List;
import java.util.function.Predicate;

@ReflectionProxy(name = "net.minecraft.world.level.EntityGetter")
public interface EntityGetterProxy {
    EntityGetterProxy INSTANCE = ASMProxyFactory.create(EntityGetterProxy.class);

    @MethodInvoker(name = "getEntitiesOfClass")
    List<Object> getEntitiesOfClass(Object target, Class<?> entityClass, @Type(clazz = AABBProxy.class) Object area, Predicate<Object> filter);
}
