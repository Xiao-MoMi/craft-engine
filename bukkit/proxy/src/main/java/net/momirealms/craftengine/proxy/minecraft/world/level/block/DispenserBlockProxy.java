package net.momirealms.craftengine.proxy.minecraft.world.level.block;

import net.momirealms.craftengine.proxy.minecraft.core.dispenser.BlockSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.core.dispenser.DispenseItemBehaviorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.ItemLikeProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.world.level.block.DispenserBlock")
public interface DispenserBlockProxy {
    DispenserBlockProxy INSTANCE = ASMProxyFactory.create(DispenserBlockProxy.class);

    @MethodInvoker(name = "registerBehavior", isStatic = true)
    void registerBehavior(@Type(clazz = ItemLikeProxy.class) Object item, @Type(clazz = DispenseItemBehaviorProxy.class) Object behavior);

    @MethodInvoker(name = "getDispenseBehavior", isStatic = true, activeIf = "has_patch=paper && min_version=1.21.2")
    Object getDispenseBehavior(@Type(clazz = BlockSourceProxy.class) Object source, @Type(clazz = ItemStackProxy.class) Object stack);

    @FieldGetter(name = "DISPENSER_REGISTRY", isStatic = true)
    Map<Object, Object> getDispenseRegistry();

    @FieldGetter(name = "eventFired", isStatic = true, activeIf = "(max_version=1.21.1 && !has_patch=folia) || !has_patch=paper")
    boolean isEventFired();

    @FieldGetter(name = "eventFired", isStatic = true, activeIf = "has_patch=folia && max_version=1.21.1")
    ThreadLocal<Boolean> eventFired();
}
