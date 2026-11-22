package net.momirealms.craftengine.proxy.minecraft.world.level.chunk;

import com.mojang.serialization.Codec;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.chunk.ChunkGenerator")
public interface ChunkGeneratorProxy {
    ChunkGeneratorProxy INSTANCE = ASMProxyFactory.create(ChunkGeneratorProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.chunk.ChunkGenerator");

    @FieldGetter(name = "CODEC", isStatic = true)
    Codec<Object> codec();

    @MethodInvoker(name = "getBiomeSource")
    Object biomeSource(Object target);

    @FieldGetter(name = "biomeSource")
    Object getBiomeSource(Object target);
}
