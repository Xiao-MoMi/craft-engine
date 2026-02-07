package net.momirealms.craftengine.proxy.minecraft.advancements;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Map;

@ReflectionProxy(name = "net.minecraft.advancements.AdvancementProgress")
public interface AdvancementProgressProxy {
    AdvancementProgressProxy INSTANCE = ASMProxyFactory.create(AdvancementProgressProxy.class);

    @ConstructorInvoker
    Object newInstance();

    @MethodInvoker(name = "update", activeIf = "min_version=1.20.2")
    void update(Object target, @Type(clazz = AdvancementRequirementsProxy.class) Object requirements);

    @MethodInvoker(name = "update", activeIf = "max_version=1.20.1")
    void update(Object target, Map<String, Object> criteria, String[][] requirements);

    @MethodInvoker(name = "grantProgress")
    boolean grantProgress(Object target, String criterionName);
}
