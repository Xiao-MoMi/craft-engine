package net.momirealms.craftengine.bukkit.reflection.bukkit.event.block;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.adventure.text.AdventureComponentProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.event.block.SignChangeEvent;

@ReflectionProxy(clazz = SignChangeEvent.class)
public interface SignChangeEventProxy {
    SignChangeEventProxy INSTANCE = ReflectionHelper.getProxy(SignChangeEventProxy.class);
    Class<?> CLAZZ = SignChangeEvent.class;

    @MethodInvoker(name = "line")
    void line(SignChangeEvent instance, int index, @Type(clazz = AdventureComponentProxy.class) Object line);
}
