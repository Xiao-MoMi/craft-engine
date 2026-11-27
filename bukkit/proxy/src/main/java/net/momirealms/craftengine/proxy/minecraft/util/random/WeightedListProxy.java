package net.momirealms.craftengine.proxy.minecraft.util.random;

import com.mojang.serialization.Codec;
import net.momirealms.craftengine.proxy.minecraft.util.RandomSourceProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.util.random.WeightedList", activeIf = "min_version=1.21.5")
public interface WeightedListProxy {
    WeightedListProxy INSTANCE = ASMProxyFactory.create(WeightedListProxy.class);

    @MethodInvoker(name = "nonEmptyCodec", isStatic = true)
    Codec<Object> nonEmptyCodec(Codec<?> codec);

    @MethodInvoker(name = "getRandomOrThrow")
    Object getRandomOrThrow(Object target, @Type(clazz = RandomSourceProxy.class) Object random);
}
