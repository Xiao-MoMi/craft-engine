package net.momirealms.craftengine.proxy.minecraft.util;

import com.mojang.datafixers.DataFixer;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.util.datafix.DataFixers")
public interface DataFixersProxy {
    DataFixersProxy INSTANCE = ASMProxyFactory.create(DataFixersProxy.class);

    @MethodInvoker(name = "getDataFixer", isStatic = true)
    DataFixer getDataFixer();
}
