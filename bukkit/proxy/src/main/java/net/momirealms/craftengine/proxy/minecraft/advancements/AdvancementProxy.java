package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Map;
import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.advancements.Advancement")
public interface AdvancementProxy {
    AdvancementProxy INSTANCE = ASMProxyFactory.create(AdvancementProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.20.2")
    Object newInstance(Optional<Object> parent,
                       Optional<Object> display,
                       @Type(clazz = AdvancementRewardsProxy.class) Object rewards,
                       Map<String, Object> criteria,
                       @Type(clazz = AdvancementRequirementsProxy.class) Object requirements,
                       boolean sendsTelemetryEvent);

    @ConstructorInvoker(activeIf = "max_version=1.20.1")
    Object newInstance(@Type(clazz = IdentifierProxy.class) Object id,
                       @Type(clazz = AdvancementProxy.class) Object parent,
                       @Type(clazz = DisplayInfoProxy.class) Object displayInfo,
                       @Type(clazz = AdvancementRewardsProxy.class) Object rewards,
                       Map<String, Object> criteria,
                       String[][] requirements,
                       boolean sendsTelemetryEvent);
}
