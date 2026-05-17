package net.momirealms.craftengine.proxy.minecraft.util.thread;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.util.thread.ProcessorHandle", activeIf = "min_version=1.21 && max_version=1.21.1")
public interface ProcessorHandleProxy {
    ProcessorHandleProxy INSTANCE = ASMProxyFactory.create(ProcessorHandleProxy.class);
}
