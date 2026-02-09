package net.momirealms.craftengine.proxy.minecraft.world;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.InteractionResult")
public interface InteractionResultProxy {
    InteractionResultProxy INSTANCE = ASMProxyFactory.create(InteractionResultProxy.class);

    @FieldGetter(name = "SUCCESS_SERVER", isStatic = true, activeIf = "min_version=1.21.2")
    Object getSuccessServer();

    @FieldGetter(name = "SUCCESS", isStatic = true)
    Object getSuccess();
}
