package net.momirealms.craftengine.bukkit.reflection.adventure.text.serializer;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.adventure.text.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net{}kyori{}adventure{}text{}serializer{}ComponentSerializer", ignoreRelocation = true)
public interface ComponentSerializerProxy {
    ComponentSerializerProxy INSTANCE = ReflectionHelper.getProxy(ComponentSerializerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ComponentSerializerProxy.class);

    @MethodInvoker(name = "serialize")
    Object serialize(Object instance, @Type(clazz = ComponentProxy.class) Object component);

    @MethodInvoker(name = "deserialize")
    Object deserialize(Object instance, Object input);
}
