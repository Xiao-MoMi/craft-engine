package net.momirealms.craftengine.proxy.minecraft.world.item.component;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.world.item.component.SwingAnimation", activeIf = "min_version=26.3")
public interface SwingAnimationProxy {
    SwingAnimationProxy INSTANCE = ASMProxyFactory.create(SwingAnimationProxy.class);
    Object DEFAULT = INSTANCE.getDefault();
    @FieldGetter(name = "DEFAULT", isStatic = true)
    Object getDefault();
}
