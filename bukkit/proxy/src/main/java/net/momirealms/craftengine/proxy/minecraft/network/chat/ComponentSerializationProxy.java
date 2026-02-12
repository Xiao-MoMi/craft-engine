package net.momirealms.craftengine.proxy.minecraft.network.chat;

import com.mojang.serialization.Codec;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.chat.ComponentSerialization", activeIf = "min_version=1.20.3")
public interface ComponentSerializationProxy {
    ComponentSerializationProxy INSTANCE = ASMProxyFactory.create(ComponentSerializationProxy.class);

    @FieldGetter(name = "CODEC", isStatic = true, activeIf = "min_version=1.20.3")
    Codec<Object> getCodec();
}
