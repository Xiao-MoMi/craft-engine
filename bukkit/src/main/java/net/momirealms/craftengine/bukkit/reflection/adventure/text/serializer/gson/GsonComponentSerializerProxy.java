package net.momirealms.craftengine.bukkit.reflection.adventure.text.serializer.gson;

import com.google.gson.Gson;
import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net{}kyori{}adventure{}text{}serializer{}gson{}GsonComponentSerializer", ignoreRelocation = true)
public interface GsonComponentSerializerProxy {
    GsonComponentSerializerProxy INSTANCE = ReflectionHelper.getProxy(GsonComponentSerializerProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(GsonComponentSerializerProxy.class);

    @MethodInvoker(name = "builder", isStatic = true)
    Object builder();

    @MethodInvoker(name = "serializer")
    Gson serializer(Object instance);

    @ReflectionProxy(name = "net{}kyori{}adventure{}text{}serializer{}gson{}GsonComponentSerializer$Builder", ignoreRelocation = true)
    interface BuilderProxy {
        BuilderProxy INSTANCE = ReflectionHelper.getProxy(BuilderProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(BuilderProxy.class);

        @MethodInvoker(name = "build")
        Object build(Object instance);
    }

    final class Constant {
        public static final Object instance = BuilderProxy.INSTANCE.build(INSTANCE.builder());
        public static final Gson serializer = INSTANCE.serializer(instance);
    }
}
