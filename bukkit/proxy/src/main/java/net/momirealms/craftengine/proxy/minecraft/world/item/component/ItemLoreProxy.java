package net.momirealms.craftengine.proxy.minecraft.world.item.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.world.item.component.ItemLore", activeIf = "min_version=1.20.5")
public interface ItemLoreProxy {
    ItemLoreProxy INSTANCE = ASMProxyFactory.create(ItemLoreProxy.class);

    @MethodInvoker(name = "styledLines")
    List<Object> getStyleLines(Object target);

    @MethodInvoker(name = "lines")
    List<Object> getLines(Object target);
}
