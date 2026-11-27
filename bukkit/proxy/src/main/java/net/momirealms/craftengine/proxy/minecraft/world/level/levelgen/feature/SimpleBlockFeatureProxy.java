package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.feature.SimpleBlockFeature", activeIf = "min_version=26.3")
public interface SimpleBlockFeatureProxy {
    SimpleBlockFeatureProxy INSTANCE = ASMProxyFactory.create(SimpleBlockFeatureProxy.class);

    @FieldGetter(name = "CODEC", isStatic = true)
    MapCodec<Object> codec();

    @MethodInvoker(name = "toPlace")
    Object toPlace(Object target);

    @MethodInvoker(name = "scheduleTick")
    boolean scheduleTick(Object target);

    @ReflectionProxy(name = "net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration", activeIf = "max_version=26.2")
    interface ConfigurationProxy {
        ConfigurationProxy INSTANCE = ASMProxyFactory.create(ConfigurationProxy.class);

        @FieldGetter(name = "CODEC", isStatic = true)
        Codec<Object> codec();

        @MethodInvoker(name = "toPlace")
        Object toPlace(Object target);

        @MethodInvoker(name = "scheduleTick", activeIf = "min_version=1.21.4")
        boolean scheduleTick(Object target);
    }
}
