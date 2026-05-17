package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.CollisionGetter")
public interface CollisionGetterProxy extends BlockGetterProxy {
    CollisionGetterProxy INSTANCE = ASMProxyFactory.create(CollisionGetterProxy.class);

    @MethodInvoker(name = "getBlockCollisions")
    Iterable<Object> getBlockCollisions(Object target,
                                        @Type(clazz = EntityProxy.class) Object entity,
                                        @Type(clazz = AABBProxy.class) Object aabb);
}
