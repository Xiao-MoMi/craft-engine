package net.momirealms.craftengine.proxy.bukkit.craftbukkit;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.Chunk;

@ReflectionProxy(name = "org.bukkit.craftbukkit.CraftChunk")
public interface CraftChunkProxy {
    CraftChunkProxy INSTANCE = ASMProxyFactory.create(CraftChunkProxy.class);

    @FieldGetter(name = {"level", "worldServer"})
    Object getWorld(Chunk target);

    @FieldSetter(name = {"level", "worldServer"})
    void setWorld(Chunk target, Object world);
}
