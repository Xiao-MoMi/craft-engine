package net.momirealms.craftengine.proxy.minecraft.world.item.trading;

import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.component.DataComponentExactPredicateProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.item.trading.ItemCost", activeIf = "min_version=1.20.5")
public interface ItemCostProxy {
    ItemCostProxy INSTANCE = ASMProxyFactory.create(ItemCostProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = HolderProxy.class) Object item, int count, @Type(clazz = DataComponentExactPredicateProxy.class) Object components);

    @FieldGetter(name = "itemStack")
    Object getItemStack(Object target);

    @FieldSetter(name = "itemStack")
    void setItemStack(Object target, Object itemStack);
}
