package net.momirealms.craftengine.bukkit.reflection.bukkit.inventory.meta;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.inventory.meta.BookMeta;

@ReflectionProxy(clazz = BookMeta.class)
public interface BookMetaProxy {
    BookMetaProxy INSTANCE = ReflectionHelper.getProxy(BookMetaProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(BookMetaProxy.class);
}
