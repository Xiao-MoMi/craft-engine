package net.momirealms.craftengine.proxy.minecraft.world.item;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentPatchProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.item.ItemStackTemplate", activeIf = "min_version=26.1")
public interface ItemStackTemplateProxy extends ItemInstanceProxy {
    ItemStackTemplateProxy INSTANCE = ASMProxyFactory.create(ItemStackTemplateProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.item.ItemStackTemplate");

    @ConstructorInvoker
    Object newInstance(@Type(clazz = HolderProxy.class) Object item, int count, @Type(clazz = DataComponentPatchProxy.class) Object components);

    @MethodInvoker(name = "create")
    Object create(Object target);

    @MethodInvoker(name = "fromNonEmptyStack", isStatic = true)
    Object fromNonEmptyStack(@Type(clazz = ItemStackProxy.class) Object stack);
}
