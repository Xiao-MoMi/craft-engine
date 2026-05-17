package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.List;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket")
public interface ClientboundCustomChatCompletionsPacketProxy {
    ClientboundCustomChatCompletionsPacketProxy INSTANCE = ASMProxyFactory.create(ClientboundCustomChatCompletionsPacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket");

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ActionProxy.class) Object action, List<String> entries);

    @FieldGetter(name = "action")
    Object getAction(Object target);

    @FieldGetter(name = "entries")
    List<String> getEntries(Object target);

    @FieldSetter(name = "entries")
    void setEntries(Object target, List<String> entries);

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket$Action")
    interface ActionProxy {
        ActionProxy INSTANCE = ASMProxyFactory.create(ActionProxy.class);
        Enum<?>[] VALUES = INSTANCE.values();
        Enum<?> ADD = VALUES[0];
        Enum<?> REMOVE = VALUES[1];
        Enum<?> SET = VALUES[2];

        @MethodInvoker(name = "values", isStatic = true)
        Enum<?>[] values();
    }
}
