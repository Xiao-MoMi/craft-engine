package net.momirealms.craftengine.proxy.minecraft.world.entity.ai.attributes;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.world.entity.ai.attributes.AttributeModifier")
public interface AttributeModifierProxy {
    AttributeModifierProxy INSTANCE = ASMProxyFactory.create(AttributeModifierProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.21")
    Object newInstance(@Type(clazz = IdentifierProxy.class) Object id,
                       double amount,
                       @Type(clazz = OperationProxy.class) Object operation);

    @ConstructorInvoker(activeIf = "max_version=1.20.6")
    Object newInstance(UUID id,
                       String name,
                       double amount,
                       @Type(clazz = OperationProxy.class) Object operation);

    @ReflectionProxy(name = "net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation")
    interface OperationProxy {
        OperationProxy INSTANCE = ASMProxyFactory.create(OperationProxy.class);
        Enum<?>[] VALUES = INSTANCE.values();
        Enum<?> ADD_VALUE = VALUES[0];
        Enum<?> ADD_MULTIPLIED_BASE = VALUES[1];
        Enum<?> ADD_MULTIPLIED_TOTAL = VALUES[2];

        @MethodInvoker(name = "values", isStatic = true)
        Enum<?>[] values();
    }
}
