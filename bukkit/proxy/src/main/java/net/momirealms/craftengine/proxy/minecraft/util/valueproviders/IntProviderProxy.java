package net.momirealms.craftengine.proxy.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.util.valueproviders.IntProvider")
public interface IntProviderProxy {
    IntProviderProxy INSTANCE = ASMProxyFactory.create(IntProviderProxy.class);

    @FieldGetter(name = "CODEC", isStatic = true, activeIf = "max_version=1.21.11")
    Codec<Object> codec();

    @MethodInvoker(name = "sample")
    int sample(Object target, @Type(clazz = RandomSourceProxy.class) Object random);
}
