package net.momirealms.craftengine.proxy.spottedleaf.dataconverter.minecraft;

import net.momirealms.craftengine.proxy.minecraft.nbt.CompoundTagProxy;
import net.momirealms.craftengine.proxy.spottedleaf.dataconverter.minecraft.datatypes.MCDataTypeProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "ca.spottedleaf.dataconverter.minecraft.MCDataConverter", activeIf = "has_patch=paper && !version=1.21.5")
public interface MCDataConverterProxy {
    MCDataConverterProxy INSTANCE = ASMProxyFactory.create(MCDataConverterProxy.class);

    @MethodInvoker(name = "convertTag", isStatic = true)
    Object convertTag(@Type(clazz = MCDataTypeProxy.class) Object type, @Type(clazz = CompoundTagProxy.class) Object data, int fromVersion, int toVersion);
}
