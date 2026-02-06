package net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.entity.ai.attributes.AttributeModifier")
public interface AttributeModifierProxy {
    AttributeModifierProxy INSTANCE = ASMProxyFactory.create(AttributeModifierProxy.class);

    @ReflectionProxy(name = "net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation")
    interface OperationProxy {
        OperationProxy INSTANCE = ASMProxyFactory.create(OperationProxy.class);
        Object[] VALUES = INSTANCE.values();
        Object ADD_VALUE = VALUES[0];
        Object ADD_MULTIPLIED_BASE = VALUES[1];
        Object ADD_MULTIPLIED_TOTAL = VALUES[2];

        @MethodInvoker(name = "values", isStatic = true)
        Object[] values();
    }
}
