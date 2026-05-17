package net.momirealms.craftengine.proxy.bukkit.craftbukkit;

import net.momirealms.craftengine.proxy.minecraft.world.level.chunk.LevelChunkProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.bukkit.Chunk;

@ReflectionProxy(name = "org.bukkit.craftbukkit.CraftChunk")
public interface CraftChunkProxy {
    CraftChunkProxy INSTANCE = ASMProxyFactory.create(CraftChunkProxy.class);

    @ConstructorInvoker
    Chunk newInstance(@Type(clazz = LevelChunkProxy.class) Object levelChunk);

    @FieldGetter(name = {"level", "worldServer"})
    Object getWorld(Chunk target);

    @FieldSetter(name = {"level", "worldServer"})
    void setWorld(Chunk target, Object world);
}
