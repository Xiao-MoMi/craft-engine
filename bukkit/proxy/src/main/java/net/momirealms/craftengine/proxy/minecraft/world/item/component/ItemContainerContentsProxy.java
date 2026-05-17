package net.momirealms.craftengine.proxy.minecraft.world.item.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.world.item.component.ItemContainerContents", activeIf = "min_version=1.20.5")
public interface ItemContainerContentsProxy {
    ItemContainerContentsProxy INSTANCE = ASMProxyFactory.create(ItemContainerContentsProxy.class);

    @MethodInvoker(name = "fromItems", isStatic = true)
    Object fromItems(List<Object> items);

    @FieldGetter(name = "items")
    List<Object> getItems(Object target);

    @FieldSetter(name = "items")
    void setItems(Object target, List<Object> items);
}
