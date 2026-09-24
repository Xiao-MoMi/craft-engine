package net.momirealms.craftengine.proxy.spottedleaf.moonrise.patches.chunk_system.scheduling.task;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.GenericDataLoadTask$TaskResult", activeIf = "min_version=1.21.4 && has_patch=paper")
public interface TaskResultProxy {
    TaskResultProxy INSTANCE = ASMProxyFactory.create(TaskResultProxy.class);

    @MethodInvoker(name = "left")
    Object left(Object target);
}
