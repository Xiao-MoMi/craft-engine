package net.momirealms.craftengine.proxy.minecraft.util;

import com.mojang.serialization.Codec;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.util.ExtraCodecs")
public interface ExtraCodecsProxy {
    ExtraCodecsProxy INSTANCE = ASMProxyFactory.create(ExtraCodecsProxy.class);

    @FieldGetter(name = "NON_EMPTY_STRING", isStatic = true)
    Codec<String> nonEmptyString();
}
