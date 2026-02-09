package net.momirealms.craftengine.proxy.minecraft.network;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Queue;

@ReflectionProxy(name = "net.minecraft.server.network.ServerConfigurationPacketListenerImpl", activeIf = "min_version=1.20.2")
public interface ServerConfigurationPacketListenerImplProxy {
    ServerConfigurationPacketListenerImplProxy INSTANCE = ASMProxyFactory.create(ServerConfigurationPacketListenerImplProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.server.network.ServerConfigurationPacketListenerImpl");

    @MethodInvoker(name = "finishCurrentTask")
    void finishCurrentTask(Object target, @Type(clazz = ConfigurationTaskProxy.TypeProxy.class) Object type);

    @MethodInvoker(name = "startNextTask")
    void startNextTask(Object target);

    @FieldGetter(name = "configurationTasks")
    Queue<Object> getConfigurationTasks(Object target);

    @MethodInvoker(name = "returnToWorld")
    void returnToWorld(Object target);
}
