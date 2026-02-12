package net.momirealms.craftengine.proxy.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.network.FriendlyByteBuf")
public interface FriendlyByteBufProxy {
    FriendlyByteBufProxy INSTANCE = ASMProxyFactory.create(FriendlyByteBufProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.FriendlyByteBuf");

    @ConstructorInvoker
    ByteBuf newInstance(ByteBuf parent);

    @MethodInvoker(name = "readItem", activeIf = "max_version=1.20.4")
    Object readItem(ByteBuf target);

    @MethodInvoker(name = "writeItem", activeIf = "max_version=1.20.4")
    void writeItem(ByteBuf target, @Type(clazz = ItemStackProxy.class) Object stack);
}
