package net.momirealms.craftengine.bedrock.geyser.proxy.session;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.geysermc.geyser.session.UpstreamSession;

@ReflectionProxy(name = "org.geysermc.geyser.session.GeyserSession")
public interface GeyserSessionProxy {
    GeyserSessionProxy INSTANCE = ASMProxyFactory.create(GeyserSessionProxy.class);

    @FieldSetter(name = "upstream")
    void upstream(Object target, UpstreamSession upstream);

    @MethodInvoker(name = "getWorldName")
    Object worldName(Object target);
}
