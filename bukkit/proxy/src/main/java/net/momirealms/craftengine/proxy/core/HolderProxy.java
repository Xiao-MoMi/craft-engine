package net.momirealms.craftengine.proxy.core;

import net.momirealms.craftengine.proxy.resource.IdentifierProxy;
import net.momirealms.craftengine.proxy.resource.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.Set;

@ReflectionProxy(name = "net.minecraft.core.Holder")
public interface HolderProxy {
    HolderProxy INSTANCE = ASMProxyFactory.create(HolderProxy.class);

    @MethodInvoker(name = "value")
    Object value(Object target);

    @MethodInvoker(name = "isBound")
    boolean isBound(Object target);

    @MethodInvoker(name = "is")
    boolean is$0(Object target, @Type(clazz = IdentifierProxy.class) Object id);

    @MethodInvoker(name = "is")
    boolean is$1(Object target, @Type(clazz = ResourceKeyProxy.class) Object id);

    @ReflectionProxy(name = "net.minecraft.core.Holder$Direct")
    interface DirectProxy extends HolderProxy {
        DirectProxy INSTANCE = ASMProxyFactory.create(DirectProxy.class);
    }

    @ReflectionProxy(name = "net.minecraft.core.Holder$Reference")
    interface ReferenceProxy extends HolderProxy {
        ReferenceProxy INSTANCE = ASMProxyFactory.create(ReferenceProxy.class);

        @FieldGetter(name = "tags")
        Set<Object> getTags(Object target);

        @FieldSetter(name = "tags")
        void setTags(Object target, Set<Object> tags);
    }
}
