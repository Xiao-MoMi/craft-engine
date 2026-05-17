package net.momirealms.craftengine.proxy.minecraft.server.dedicated;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.dedicated.DedicatedServerProperties")
public interface DedicatedServerPropertiesProxy {
    DedicatedServerPropertiesProxy INSTANCE = ASMProxyFactory.create(DedicatedServerPropertiesProxy.class);

    @FieldGetter(name = "enforceSecureProfile")
    boolean isEnforceSecureProfile(Object target);

    @FieldSetter(name = "enforceSecureProfile")
    void setEnforceSecureProfile(Object target, boolean value);
}
