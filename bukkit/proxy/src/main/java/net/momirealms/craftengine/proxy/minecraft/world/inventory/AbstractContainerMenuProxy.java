package net.momirealms.craftengine.proxy.minecraft.world.inventory;

import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.inventory.AbstractContainerMenu")
public interface AbstractContainerMenuProxy {
    AbstractContainerMenuProxy INSTANCE = ASMProxyFactory.create(AbstractContainerMenuProxy.class);

    @FieldGetter(name = "containerId")
    int getContainerId(Object target);

    @FieldGetter(name = "menuType")
    Object getMenuType(Object target);

    @MethodInvoker(name = "broadcastFullState")
    void broadcastFullState(Object target);

    @FieldGetter(name = "checkReachable")
    boolean getCheckReachable(Object target);

    @FieldSetter(name = "checkReachable")
    void setCheckReachable(Object target, boolean checkReachable);

    @MethodInvoker(name = "quickMoveStack")
    Object quickMoveStack(Object target, @Type(clazz = PlayerProxy.class) Object player, int slot);

    @MethodInvoker(name = "broadcastChanges")
    void broadcastChanges(Object target);
}
