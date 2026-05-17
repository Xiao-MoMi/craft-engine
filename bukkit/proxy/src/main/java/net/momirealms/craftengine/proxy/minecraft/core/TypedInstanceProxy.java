package net.momirealms.craftengine.proxy.minecraft.core;

import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.TagKeyProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.stream.Stream;

@ReflectionProxy(name = "net.minecraft.core.TypedInstance", activeIf = "min_version=26.1")
public interface TypedInstanceProxy {
    TypedInstanceProxy INSTANCE = ASMProxyFactory.create(TypedInstanceProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.core.TypedInstance");

    @MethodInvoker(name = "typeHolder")
    Object typeHolder(Object target);

    @MethodInvoker(name = "tags")
    Stream<Object> tags(Object target);

    @MethodInvoker(name = "is")
    boolean is$0(Object target, Object rawType);

    @MethodInvoker(name = "is")
    boolean is$1(Object target, @Type(clazz = TagKeyProxy.class) Object tag);

    @MethodInvoker(name = "is")
    boolean is$2(Object target, @Type(clazz = HolderSetProxy.class) Iterable<?> set);

    @MethodInvoker(name = "is")
    boolean is$3(Object target, @Type(clazz = HolderProxy.class) Object type);

    @MethodInvoker(name = "is")
    boolean is$4(Object target, @Type(clazz = ResourceKeyProxy.class) Object type);

}
