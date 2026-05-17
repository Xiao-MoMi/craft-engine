package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;
import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ServerboundEditBookPacket")
public interface ServerboundEditBookPacketProxy {
    ServerboundEditBookPacketProxy INSTANCE = ASMProxyFactory.create(ServerboundEditBookPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ServerboundEditBookPacket");

    @FieldGetter(name = "slot")
    int getSlot(Object target);

    @FieldGetter(name = "pages")
    List<String> getPages(Object target);

    @FieldGetter(name = "title")
    Optional<String> getTitle(Object target);

    @ConstructorInvoker
    Object newInstance(int slot, List<String> pages, Optional<String> title);
}
