package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
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
    boolean is$0(Object target, @Type(clazz = IdentifierProxy.class) Object identifier);

    @MethodInvoker(name = "is")
    boolean is$1(Object target, @Type(clazz = ResourceKeyProxy.class) Object resourceKey);

    @MethodInvoker(name = "getRegisteredName", activeIf = "min_version=1.20.5")
    String getRegisteredName(Object target);

    @MethodInvoker(name = "direct", isStatic = true)
    Object direct(Object value);

    @ReflectionProxy(name = "net.minecraft.core.Holder$Direct")
    interface DirectProxy extends HolderProxy {
        DirectProxy INSTANCE = ASMProxyFactory.create(DirectProxy.class);
    }

    @ReflectionProxy(name = "net.minecraft.core.Holder$Reference")
    interface ReferenceProxy extends HolderProxy {
        ReferenceProxy INSTANCE = ASMProxyFactory.create(ReferenceProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.core.Holder$Reference");

        @FieldGetter(name = "tags")
        Set<Object> getTags(Object target);

        @FieldSetter(name = "tags")
        void setTags(Object target, Set<Object> tags);

        @MethodInvoker(name = "bindValue")
        void bindValue(Object target, Object value);

        @FieldGetter(name = "key")
        Object getKey(Object target);
    }
}
