package net.momirealms.craftengine.proxy.minecraft.network.codec;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.network.codec.IdDispatchCodec", activeIf = "min_version=1.20.5")
public interface IdDispatchCodecProxy extends StreamCodecProxy {
    IdDispatchCodecProxy INSTANCE = ASMProxyFactory.create(IdDispatchCodecProxy.class);

    @FieldGetter(name = "byId")
    List<Object> getById(Object target);

    @ReflectionProxy(name = "net.minecraft.network.codec.IdDispatchCodec$Entry", activeIf = "min_version=1.20.5")
    interface EntryProxy {
        EntryProxy INSTANCE = ASMProxyFactory.create(EntryProxy.class);

        @FieldGetter(name = "type")
        Object getType(Object target);
    }
}
