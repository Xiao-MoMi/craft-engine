package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.entries;

import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.entries.UniformContainerBase", activeIf = "min_version=26.3")
public interface UniformContainerBaseProxy {
    UniformContainerBaseProxy INSTANCE = ASMProxyFactory.create(UniformContainerBaseProxy.class);

    @MethodInvoker(name = "uniformFields", isStatic = true)
    <T> Products.P4<RecordCodecBuilder.Mu<T>, Integer, Integer, Object, Object> uniformFields(RecordCodecBuilder.Instance<T> instance);
}
