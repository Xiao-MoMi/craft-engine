package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.feature.ConfiguredFeature")
public interface ConfiguredFeatureProxy {
    ConfiguredFeatureProxy INSTANCE = ASMProxyFactory.create(ConfiguredFeatureProxy.class);
    Codec<Object> CODEC = INSTANCE.getCodec();

    @FieldGetter(name = "CODEC", isStatic = true)
    Codec<Object> getCodec();
}
