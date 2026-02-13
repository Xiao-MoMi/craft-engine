package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.CommonPlayerSpawnInfo", activeIf = "min_version=1.20.2")
public interface CommonPlayerSpawnInfoProxy {
    CommonPlayerSpawnInfoProxy INSTANCE = ASMProxyFactory.create(CommonPlayerSpawnInfoProxy.class);

    @FieldGetter(name = "dimension")
    Object getDimension(Object target);
}
