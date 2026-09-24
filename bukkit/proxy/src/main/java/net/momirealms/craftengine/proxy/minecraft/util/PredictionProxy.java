package net.momirealms.craftengine.proxy.minecraft.util;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

@ReflectionProxy(name = "net.minecraft.util.Prediction", activeIf = "min_version=26.3")
public interface PredictionProxy {
    PredictionProxy INSTANCE = ASMProxyFactory.create(PredictionProxy.class);
    Object SERVER_ONLY = INSTANCE.getServerOnly();
    @FieldGetter(name = "SERVER_ONLY", isStatic = true)
    Object getServerOnly();
}
