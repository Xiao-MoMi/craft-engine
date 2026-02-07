package net.momirealms.craftengine.proxy.minecraft.server.network;

import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.server.network.ServerGamePacketListenerImpl")
public interface ServerGamePacketListenerImplProxy {
    ServerGamePacketListenerImplProxy INSTANCE = ASMProxyFactory.create(ServerGamePacketListenerImplProxy.class);

    @MethodInvoker(name = "tryPickItem", activeIf = "min_version=1.21.5")
    void tryPickItem(Object target,
                     @Type(clazz = ItemStackProxy.class) Object item,
                     @Type(clazz = BlockPosProxy.class) Object pos,
                     @Type(clazz = EntityProxy.class) Object entity,
                     boolean includeData);

    @MethodInvoker(name = "tryPickItem", activeIf = "version=1.21.4")
    void tryPickItem(Object target,
                     @Type(clazz = ItemStackProxy.class) Object item);
}
