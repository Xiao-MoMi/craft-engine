package net.momirealms.craftengine.proxy.spottedleaf.dataconverter.minecraft.datatypes;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry", activeIf = "has_patch=paper && !version=1.21.5")
public interface MCTypeRegistryProxy {
    MCTypeRegistryProxy INSTANCE = ASMProxyFactory.create(MCTypeRegistryProxy.class);
    Object ITEM_STACK = INSTANCE != null ? INSTANCE.getItemStack() : null;

    @FieldGetter(name = "ITEM_STACK", isStatic = true)
    Object getItemStack();
}
