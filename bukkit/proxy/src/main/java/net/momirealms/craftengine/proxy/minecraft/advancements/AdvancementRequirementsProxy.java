package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.advancements.AdvancementRequirements")
public interface AdvancementRequirementsProxy {
    AdvancementRequirementsProxy INSTANCE = ASMProxyFactory.create(AdvancementRequirementsProxy.class);

    @ConstructorInvoker(activeIf = "min_version=1.20.3")
    Object newInstance(List<List<String>> requirements);

    @ConstructorInvoker(activeIf = "max_version=1.20.2")
    Object newInstance(String[][] requirements);
}
