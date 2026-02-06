package net.momirealms.craftengine.proxy.minecraft.resources;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {"net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation"})
public interface IdentifierProxy {
    IdentifierProxy INSTANCE = ASMProxyFactory.create(IdentifierProxy.class);

    @MethodInvoker(name = "fromNamespaceAndPath", isStatic = true)
    Object fromNamespaceAndPath(String namespace, String path);

    @FieldGetter(name = "namespace")
    String getNamespace(Object target);

    @FieldGetter(name = "path")
    String getPath(Object target);
}
