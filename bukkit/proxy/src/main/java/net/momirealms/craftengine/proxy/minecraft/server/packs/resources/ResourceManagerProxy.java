package net.momirealms.craftengine.proxy.minecraft.server.packs.resources;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.Map;
import java.util.function.Predicate;

@ReflectionProxy(name = "net.minecraft.server.packs.resources.ResourceManager")
public interface ResourceManagerProxy {
    ResourceManagerProxy INSTANCE = ASMProxyFactory.create(ResourceManagerProxy.class);

    @MethodInvoker(name = "listResources", activeIf = "max_version=26.2")
    default Map<Object, Object> listResources(Object target, String path, Predicate<Object> filter) {
        Object selector = java.lang.reflect.Proxy.newProxyInstance(SelectorProxy.CLASS.getClassLoader(), new Class<?>[]{SelectorProxy.CLASS}, (proxy, method, args) -> filter.test(args[0]));
        return listResources26_3(target, path, selector);
    }

    @MethodInvoker(name = "listResources", activeIf = "min_version=26.3")
    Map<Object, Object> listResources26_3(Object target, String path, @net.momirealms.sparrow.reflection.proxy.annotation.Type(clazz = SelectorProxy.class) Object selector);

    @ReflectionProxy(name = "net.minecraft.server.packs.resources.ResourceManager$Selector", activeIf = "min_version=26.3")
    interface SelectorProxy {
        Class<?> CLASS = net.momirealms.sparrow.reflection.clazz.SparrowClass.find("net.minecraft.server.packs.resources.ResourceManager$Selector");
    }
}
