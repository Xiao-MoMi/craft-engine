package net.momirealms.craftengine.proxy.bukkit.inventory.meta;

import net.momirealms.craftengine.proxy.adventure.text.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.inventory.meta.BookMeta;

@ReflectionProxy(clazz = BookMeta.class)
public interface BookMetaProxy {
    BookMetaProxy INSTANCE = ASMProxyFactory.create(BookMetaProxy.class);

    @MethodInvoker(name = "page")
    Object page(Object target, int page);

    @MethodInvoker(name = "page")
    void page(Object target, int page, @Type(clazz = ComponentProxy.class) Object value);
}
