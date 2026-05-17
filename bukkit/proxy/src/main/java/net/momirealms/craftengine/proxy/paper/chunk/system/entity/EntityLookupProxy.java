package net.momirealms.craftengine.proxy.paper.chunk.system.entity;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = {"io.papermc.paper.chunk.system.entity.EntityLookup", "ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup"}, activeIf = "has_patch=paper")
public interface EntityLookupProxy {
    EntityLookupProxy INSTANCE = ASMProxyFactory.create(EntityLookupProxy.class);

    @FieldGetter(name = "worldCallback")
    Object getWorldCallback(Object target);

    @FieldSetter(name = "worldCallback")
    void setWorldCallback(Object target, Object value);

    @MethodInvoker(name = "get")
    Object get(Object target, int id);

    @MethodInvoker(name = "getChunk")
    Object getChunk(Object target, int chunkX, int chunkZ);

    @MethodInvoker(name = "canRemoveEntity")
    boolean canRemoveEntity(Object target, @Type(clazz = EntityProxy.class) Object entity);
}
