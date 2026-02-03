package net.momirealms.craftengine.bukkit.reflection.bukkit.inventory.meta;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.adventure.text.AdventureComponentProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.inventory.meta.BookMeta;

@ReflectionProxy(clazz = BookMeta.class)
public interface BookMetaProxy {
    BookMetaProxy INSTANCE = ReflectionHelper.getProxy(BookMetaProxy.class);
    Class<?> CLAZZ = BookMeta.class;

    @MethodInvoker(name = "page")
    void page(BookMeta instance, int page, @Type(clazz = AdventureComponentProxy.class) Object data);
}
