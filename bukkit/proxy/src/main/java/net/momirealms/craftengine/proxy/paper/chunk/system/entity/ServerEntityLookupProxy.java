package net.momirealms.craftengine.proxy.paper.chunk.system.entity;

import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "ca.spottedleaf.moonrise.patches.chunk_system.level.entity.server.ServerEntityLookup", activeIf = "has_patch=paper && min_version=1.21")
public interface ServerEntityLookupProxy {
    Class<?> CLASS = SparrowClass.find("ca.spottedleaf.moonrise.patches.chunk_system.level.entity.server.ServerEntityLookup");
    ServerEntityLookupProxy INSTANCE = ASMProxyFactory.create(ServerEntityLookupProxy.class);

    @FieldGetter(name = "trackerEntities", activeIf = "!has_patch=folia && max_version=26.1.2", optional = true)
    Object getTrackerEntities(Object target);

    @MethodInvoker(name = "entityEndLoaded", activeIf = "has_patch=folia && min_version=1.21.4 && max_version=26.1.2")
    void removeRegionTracker(Object target, @Type(clazz = EntityProxy.class) Object entity);

    @ReflectionProxy(name = "ca.spottedleaf.moonrise.common.list.ReferenceList", activeIf = "has_patch=paper && min_version=1.21")
    interface ReferenceListProxy {
        ReferenceListProxy INSTANCE = ASMProxyFactory.create(ReferenceListProxy.class);

        @MethodInvoker(name = "remove")
        boolean remove(Object target, Object entity);
    }
}
