package net.momirealms.craftengine.proxy.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.util.valueproviders.IntProviders", activeIf = "min_version=26.1")
public interface IntProvidersProxy {
    IntProvidersProxy INSTANCE = ASMProxyFactory.create(IntProvidersProxy.class);

    @FieldGetter(name = "CODEC", isStatic = true)
    Codec<Object> codec();
}
