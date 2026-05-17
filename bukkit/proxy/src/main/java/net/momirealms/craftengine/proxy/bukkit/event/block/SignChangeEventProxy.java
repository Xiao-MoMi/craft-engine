package net.momirealms.craftengine.proxy.bukkit.event.block;

import net.momirealms.craftengine.proxy.adventure.text.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.bukkit.event.block.SignChangeEvent;

import java.util.List;

@ReflectionProxy(clazz = SignChangeEvent.class)
public interface SignChangeEventProxy {
    SignChangeEventProxy INSTANCE = ASMProxyFactory.create(SignChangeEventProxy.class);

    @FieldGetter(name = "adventure$lines")
    List<Object> getAdventure$lines(Object target);

    @MethodInvoker(name = "line")
    void line(Object target, int index, @Type(clazz = ComponentProxy.class) Object line);
}
