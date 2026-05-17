package net.momirealms.craftengine.proxy.minecraft.tags;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.tags.TagKey")
public interface TagKeyProxy {
    TagKeyProxy INSTANCE = ASMProxyFactory.create(TagKeyProxy.class);

    @FieldGetter(name = "registry")
    Object getRegistry(Object target);

    @FieldGetter(name = "location")
    Object getLocation(Object target);

    @MethodInvoker(name = "create", isStatic = true)
    Object create(@Type(clazz = ResourceKeyProxy.class) Object registry, @Type(clazz = IdentifierProxy.class) Object location);
}