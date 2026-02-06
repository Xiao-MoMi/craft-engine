package net.momirealms.craftengine.proxy.minecraft.server.dedicated;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.dedicated.DedicatedServerSettings")
public interface DedicatedServerSettingsProxy {
    DedicatedServerSettingsProxy INSTANCE = ASMProxyFactory.create(DedicatedServerSettingsProxy.class);

    @FieldGetter(name = "properties")
    Object getProperties(Object target);
}
