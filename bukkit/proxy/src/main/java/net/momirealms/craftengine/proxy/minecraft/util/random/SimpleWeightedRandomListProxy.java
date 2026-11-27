package net.momirealms.craftengine.proxy.minecraft.util.random;

import com.mojang.serialization.Codec;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.util.Optional;

@ReflectionProxy(name = "net.minecraft.util.random.SimpleWeightedRandomList", activeIf = "max_version=1.21.4")
public interface SimpleWeightedRandomListProxy {
    SimpleWeightedRandomListProxy INSTANCE = ASMProxyFactory.create(SimpleWeightedRandomListProxy.class);

    @MethodInvoker(name = "wrappedCodec", isStatic = true)
    Codec<Object> wrappedCodec(Codec<?> codec);

    @MethodInvoker(name = "getRandomValue")
    Optional<Object> getRandomValue(Object target, @Type(clazz = RandomSourceProxy.class) Object random);
}
