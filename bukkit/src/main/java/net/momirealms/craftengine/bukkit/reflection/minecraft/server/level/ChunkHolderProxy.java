package net.momirealms.craftengine.bukkit.reflection.minecraft.server.level;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.world.level.ChunkPosProxy;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.server.level.ChunkHolder")
public interface ChunkHolderProxy {
    ChunkHolderProxy INSTANCE = ReflectionHelper.getProxy(ChunkHolderProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ChunkHolderProxy.class);

    @FieldGetter(name = "playerProvider")
    Object playerProvider(Object instance);

    @FieldSetter(name = "playerProvider", strategy = Strategy.MH)
    void playerProvider(Object instance, Object playerProvider);

    @ReflectionProxy(name = "net.minecraft.server.level.ChunkHolder$PlayerProvider")
    interface PlayerProviderProxy {
        PlayerProviderProxy INSTANCE = ReflectionHelper.getProxy(PlayerProviderProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(PlayerProviderProxy.class);

        @MethodInvoker(name = "getPlayers")
        List<Object> getPlayers(Object instance, @Type(clazz = ChunkPosProxy.class) Object chunkPos, boolean onlyOnWatchDistanceEdge);
    }
}
