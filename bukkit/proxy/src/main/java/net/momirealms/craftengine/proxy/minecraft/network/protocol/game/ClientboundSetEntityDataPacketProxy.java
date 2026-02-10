package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.RegistryFriendlyByteBufProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket")
public interface ClientboundSetEntityDataPacketProxy {
    ClientboundSetEntityDataPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundSetEntityDataPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(int id, List<?> packedItems);

    @MethodInvoker(name = "pack", isStatic = true, activeIf = "max_version=1.20.4")
    void pack$0(List<?> trackedValues, @Type(clazz = FriendlyByteBufProxy.class) Object buf);

    @MethodInvoker(name = "unpack", isStatic = true, activeIf = "max_version=1.20.4")
    List<Object> unpack$0(@Type(clazz = FriendlyByteBufProxy.class) Object buf);

    @MethodInvoker(name = "pack", isStatic = true, activeIf = "min_version=1.20.5")
    void pack$1(List<?> trackedValues, @Type(clazz = RegistryFriendlyByteBufProxy.class) Object buf);

    @MethodInvoker(name = "unpack", isStatic = true, activeIf = "min_version=1.20.5")
    List<Object> unpack$1(@Type(clazz = RegistryFriendlyByteBufProxy.class) Object buf);
}
