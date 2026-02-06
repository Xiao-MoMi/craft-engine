package net.momirealms.craftengine.proxy.minecraft.util;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap")
public interface CrudeIncrementalIntIdentityHashBiMapProxy {
    CrudeIncrementalIntIdentityHashBiMapProxy INSTANCE = ASMProxyFactory.create(CrudeIncrementalIntIdentityHashBiMapProxy.class);

    @FieldGetter(name = "keys")
    Object[] getKeys(Object target);
}
