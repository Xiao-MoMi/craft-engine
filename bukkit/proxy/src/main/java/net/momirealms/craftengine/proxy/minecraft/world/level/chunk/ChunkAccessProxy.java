package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.ChunkAccess")
public interface ChunkAccessProxy {
    ChunkAccessProxy INSTANCE = ASMProxyFactory.create(ChunkAccessProxy.class);

    @FieldGetter(name = "blockEntities", activeIf = "max_version=1.21.10 || !has_patch=canvas")
    Map<Object, Object> getBlockEntities(Object target);

    @FieldSetter(name = "blockEntities", activeIf = "max_version=1.21.10 || !has_patch=canvas")
    void setBlockEntities(Object target, Map<Object, Object> value);

    @MethodInvoker(name = "canvas$getAllBlockEntities", activeIf = "min_version=1.21.11 && has_patch=canvas")
    Object[] canvas$getAllBlockEntities(Object target);

    @MethodInvoker(name = "isUnsaved")
    boolean isUnsaved(Object target);

    @MethodInvoker(name = "getSections")
    Object[] getSections(Object target);

    @MethodInvoker(name = "setUnsaved", activeIf = "max_version=1.21.1")
    void setUnsaved(Object target, boolean needsSaving);

    @FieldGetter(name = "chunkPos")
    Object getChunkPos(Object target);

    @FieldGetter(name = "persistentDataContainer")
    PersistentDataContainer getPersistentDataContainer(Object target);
}
