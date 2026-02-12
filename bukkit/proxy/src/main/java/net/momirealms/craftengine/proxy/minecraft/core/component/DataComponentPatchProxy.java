package net.momirealms.craftengine.proxy.minecraft.core.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.core.component.DataComponentPatch", activeIf = "min_version=1.20.5")
public interface DataComponentPatchProxy {
    DataComponentPatchProxy INSTANCE = ASMProxyFactory.create(DataComponentPatchProxy.class);
}
