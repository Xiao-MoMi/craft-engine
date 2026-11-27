package net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType", activeIf = "max_version=26.2")
public interface BlockStateProviderTypeProxy {
    BlockStateProviderTypeProxy INSTANCE = ASMProxyFactory.create(BlockStateProviderTypeProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType");

    @ConstructorInvoker(activeIf = "max_version=1.20.4")
    Object newInstance(Codec<?> codec);

    @ConstructorInvoker(activeIf = "min_version=1.20.5")
    Object newInstance(MapCodec<?> codec);
}
