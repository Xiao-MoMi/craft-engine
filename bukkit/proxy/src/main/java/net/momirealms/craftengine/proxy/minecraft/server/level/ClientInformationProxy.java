package net.momirealms.craftengine.proxy.minecraft.server.level;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.server.level.ClientInformation", activeIf = "min_version=1.20.2")
public interface ClientInformationProxy {
    ClientInformationProxy INSTANCE = ASMProxyFactory.create(ClientInformationProxy.class);

    @FieldGetter(name = "language")
    String getLanguage(Object target);
}
