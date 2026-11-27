package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen;

import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.WorldgenRandom")
public interface WorldgenRandomProxy {
    WorldgenRandomProxy INSTANCE = ASMProxyFactory.create(WorldgenRandomProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = RandomSourceProxy.class) Object random);
}
