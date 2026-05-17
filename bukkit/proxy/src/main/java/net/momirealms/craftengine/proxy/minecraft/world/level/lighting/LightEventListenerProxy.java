package net.momirealms.craftengine.proxy.minecraft.world.level.lighting;

import net.momirealms.craftengine.proxy.minecraft.core.SectionPosProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.lighting.LightEventListener")
public interface LightEventListenerProxy {
    LightEventListenerProxy INSTANCE = ASMProxyFactory.create(LightEventListenerProxy.class);

    @MethodInvoker(name = "updateSectionStatus")
    void updateSectionStatus(Object target, @Type(clazz = SectionPosProxy.class) Object pos, boolean notReady);
}
