package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.entries;

import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = {
        "net.minecraft.world.level.storage.loot.entries.SingleEntryContainerBase",
        "net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer"
})
public interface SingleEntryContainerBaseProxy {
    SingleEntryContainerBaseProxy INSTANCE = ASMProxyFactory.create(SingleEntryContainerBaseProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.storage.loot.entries.SingleEntryContainerBase", "net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer");

    @MethodInvoker(name = "singletonFields", isStatic = true, activeIf = "min_version=1.20.2 && max_version=26.2")
    <T> Products.P4<RecordCodecBuilder.Mu<T>, Integer, Integer, Object, Object> singletonFields(RecordCodecBuilder.Instance<T> instance);

    @ReflectionProxy(name = "net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer$Serializer", activeIf = "max_version=1.20.1")
    interface SerializerProxy {
        Class<?> CLASS = SparrowClass.find("net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer$Serializer");
    }
}
