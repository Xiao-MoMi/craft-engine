package net.momirealms.craftengine.proxy.leaf.config;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "org.dreeam.leaf.config.modules.opt.SleepingBlockEntity", optional = true)
public interface SleepingBlockEntityProxy {
    SleepingBlockEntityProxy INSTANCE = ASMProxyFactory.create(SleepingBlockEntityProxy.class);
    Class<?> CLASS = SparrowClass.find("org.dreeam.leaf.config.modules.opt.SleepingBlockEntity");

    @FieldGetter(name = "enabled", isStatic = true)
    boolean isEnabled();
}
