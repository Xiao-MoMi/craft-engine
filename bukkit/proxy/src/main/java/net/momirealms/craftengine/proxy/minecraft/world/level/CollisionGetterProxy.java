package net.momirealms.craftengine.proxy.minecraft.world.level;

import net.momirealms.craftengine.proxy.minecraft.world.phys.shapes.CollisionContextProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.CollisionGetter")
public interface CollisionGetterProxy extends BlockGetterProxy {
    CollisionContextProxy INSTANCE = ASMProxyFactory.create(CollisionContextProxy.class);

}
