package net.momirealms.craftengine.proxy.minecraft.world.item.component;

import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackTemplateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.component.UseRemainder", activeIf = "min_version=1.21.2")
public interface UseRemainderProxy {
    UseRemainderProxy INSTANCE = ASMProxyFactory.create(UseRemainderProxy.class);

    @FieldGetter(name = "convertInto")
    Object getConvertInto(Object target);

    @ConstructorInvoker(activeIf = "max_version=1.21.11")
    Object newInstance$0(@Type(clazz = ItemStackProxy.class) Object convertInto);

    @ConstructorInvoker(activeIf = "min_version=26.1")
    Object newInstance$1(@Type(clazz = ItemStackTemplateProxy.class) Object convertInto);

}
