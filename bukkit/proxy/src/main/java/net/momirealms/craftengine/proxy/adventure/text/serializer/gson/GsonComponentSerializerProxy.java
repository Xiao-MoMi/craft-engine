package net.momirealms.craftengine.proxy.adventure.text.serializer.gson;

import com.google.gson.Gson;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net{}kyori{}adventure{}text{}serializer{}gson{}GsonComponentSerializer", ignoreRelocation = true)
public interface GsonComponentSerializerProxy {
    GsonComponentSerializerProxy INSTANCE = ASMProxyFactory.create(GsonComponentSerializerProxy.class);
    Gson GSON = INSTANCE.serializer(INSTANCE.gson());

    @MethodInvoker(name = "builder", isStatic = true)
    Object builder();

    @MethodInvoker(name = "gson", isStatic = true)
    Object gson();

    @MethodInvoker(name = "serializer")
    Gson serializer(Object target);

    @ReflectionProxy(name = "net{}kyori{}adventure{}text{}serializer{}gson{}GsonComponentSerializer$Builder", ignoreRelocation = true)
    interface BuilderProxy {
        BuilderProxy INSTANCE = ASMProxyFactory.create(BuilderProxy.class);

        @MethodInvoker(name = "build")
        Object build(Object target);
    }
}
