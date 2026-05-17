package net.momirealms.craftengine.proxy.minecraft.network.chat;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;

@ReflectionProxy(name = "net.minecraft.network.chat.SignedMessageBody")
public interface SignedMessageBodyProxy {
    SignedMessageBodyProxy INSTANCE = ASMProxyFactory.create(SignedMessageBodyProxy.class);

    @ReflectionProxy(name = "net.minecraft.network.chat.SignedMessageBody$Packed")
    interface PackedProxy {
        PackedProxy INSTANCE = ASMProxyFactory.create(PackedProxy.class);

        @FieldGetter(name = "content")
        String getContent(Object target);
    }
}
