package net.momirealms.craftengine.proxy.minecraft.core.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.component.DataComponentPatch")
public interface DataComponentPatchProxy {
    DataComponentPatchProxy INSTANCE = ASMProxyFactory.create(DataComponentPatchProxy.class);
}
