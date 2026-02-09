package net.momirealms.craftengine.proxy.minecraft.server;

import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.server.MinecraftServer")
public interface MinecraftServerProxy {
    MinecraftServerProxy INSTANCE = ASMProxyFactory.create(MinecraftServerProxy.class);

    @MethodInvoker(name = "getServer", isStatic = true)
    Object getServer();

    @MethodInvoker(name = "getPackRepository")
    Object getPackRepository(Object target);

    @FieldGetter(name = "connection")
    Object getConnection(Object target);

    @FieldGetter(name = "scoreboard")
    Object getScoreboard(Object target);

    @MethodInvoker(name = "getRecipeManager")
    Object getRecipeManager(Object target);

    @MethodInvoker(name = "registryAccess")
    Object registryAccess(Object target);

    @ReflectionProxy(name = "net.minecraft.server.MinecraftServer$ServerResourcePackInfo")
    interface ServerResourcePackInfoProxy {
        ServerResourcePackInfoProxy INSTANCE = ASMProxyFactory.create(ServerResourcePackInfoProxy.class);

        @ConstructorInvoker(activeIf = "min_version=1.20.3")
        Object newInstance(UUID id, String url, String hash, boolean isRequired, @Nullable @Type(clazz = ComponentProxy.class) Object prompt);

        @ConstructorInvoker(activeIf = "max_version=1.20.2")
        Object newInstance(String url, String hash, boolean isRequired, @Nullable @Type(clazz = ComponentProxy.class) Object prompt);
    }
}
