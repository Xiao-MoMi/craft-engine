package net.momirealms.craftengine.proxy.minecraft.world.item.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.world.item.component.BundleContents", activeIf = "min_version=1.20.5")
public interface BundleContentsProxy {
    BundleContentsProxy INSTANCE = ASMProxyFactory.create(BundleContentsProxy.class);

    @ConstructorInvoker
    Object newInstance(List<Object> items);

    @FieldGetter(name = "items")
    List<Object> getItems(Object target);

    @FieldSetter(name = "items")
    void setItems(Object target, List<Object> items);
}
