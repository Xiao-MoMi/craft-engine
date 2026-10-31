package net.momirealms.craftengine.bedrock.geyser.proxy;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.geysermc.geyser.GeyserBootstrap;

@ReflectionProxy(name = "org.geysermc.geyser.GeyserImpl")
public interface GeyserBootstrapProxy {
    GeyserBootstrapProxy INSTANCE = ASMProxyFactory.create(GeyserBootstrapProxy.class);

    @FieldSetter(name = "bootstrap")
    void bootstrap(Object instance, GeyserBootstrap bootstrap);
}
