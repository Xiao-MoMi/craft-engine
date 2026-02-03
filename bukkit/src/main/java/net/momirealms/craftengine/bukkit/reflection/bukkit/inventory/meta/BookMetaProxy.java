package net.momirealms.craftengine.bukkit.reflection.bukkit.inventory.meta;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.adventure.text.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.inventory.meta.BookMeta;

@ReflectionProxy(clazz = BookMeta.class)
public interface BookMetaProxy {
    BookMetaProxy INSTANCE = ReflectionHelper.getProxy(BookMetaProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(BookMetaProxy.class);

    @MethodInvoker(name = "page")
    void page(BookMeta instance, int page, @Type(clazz = ComponentProxy.class) Object data);
}
