package net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

@ReflectionProxy(name = "net.minecraft.world.level.storage.loot.entries.LootPoolEntryType", activeIf = "max_version=1.21.11")
public interface LootPoolEntryTypeProxy {
    LootPoolEntryTypeProxy INSTANCE = ASMProxyFactory.create(LootPoolEntryTypeProxy.class);

    @ConstructorInvoker(activeIf = "max_version=1.20.1")
    Object newInstance(@Type(clazz = SerializerProxy.class) Object serializer);

    @ConstructorInvoker(activeIf = "min_version=1.20.2 && max_version=1.20.4")
    Object newInstance(Codec<?> codec);

    @ConstructorInvoker(activeIf = "min_version=1.20.5")
    Object newInstance(MapCodec<?> codec);

    @ReflectionProxy(name = "net.minecraft.world.level.storage.loot.Serializer", activeIf = "max_version=1.20.1")
    interface SerializerProxy {
    }
}
