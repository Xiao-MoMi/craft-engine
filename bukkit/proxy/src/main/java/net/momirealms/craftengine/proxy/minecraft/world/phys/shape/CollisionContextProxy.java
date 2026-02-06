package net.momirealms.craftengine.proxy.minecraft.world.phys.shape;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.phys.shapes.CollisionContext")
public interface CollisionContextProxy {
    CollisionContextProxy INSTANCE = ASMProxyFactory.create(CollisionContextProxy.class);

    @MethodInvoker(name = "empty", isStatic = true)
    Object empty();

    @MethodInvoker(name = "of", isStatic = true)
    Object of(@Type(clazz = EntityProxy.class) Object entity);

    @MethodInvoker(name = "placementContext", isStatic = true, activeIf = "min_version=1.21.6")
    Object placementContext(@Type(clazz = PlayerProxy.class) Object player);
}
