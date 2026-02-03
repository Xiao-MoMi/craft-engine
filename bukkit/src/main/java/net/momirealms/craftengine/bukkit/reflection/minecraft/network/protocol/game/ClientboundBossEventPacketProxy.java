package net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.minecraft.network.chat.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundBossEventPacket")
public interface ClientboundBossEventPacketProxy {
    ClientboundBossEventPacketProxy INSTANCE = ReflectionHelper.getProxy(ClientboundBossEventPacketProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ClientboundBossEventPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(UUID id, @Type(clazz = OperationProxy.class) Object operation);

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundBossEventPacket$Operation")
    interface OperationProxy {
        OperationProxy INSTANCE = ReflectionHelper.getProxy(OperationProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(OperationProxy.class);
    }

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundBossEventPacket$AddOperation")
    interface AddOperationProxy {
        AddOperationProxy INSTANCE = ReflectionHelper.getProxy(AddOperationProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(AddOperationProxy.class);

        @ConstructorInvoker(strategy = Strategy.UNSAFE)
        Object newInstance();

        @FieldGetter(name = "name")
        Object name(Object instance);

        @FieldSetter(name = "name", strategy = Strategy.MH)
        void name(Object instance, Object name);

        @FieldGetter(name = "progress")
        float progress(Object instance);

        @FieldSetter(name = "progress", strategy = Strategy.MH)
        void progress(Object instance, float progress);

        @FieldGetter(name = "color")
        Object color(Object instance);

        @FieldSetter(name = "color", strategy = Strategy.MH)
        void color(Object instance, Object color);

        @FieldGetter(name = "overlay")
        Object overlay(Object instance);

        @FieldSetter(name = "overlay", strategy = Strategy.MH)
        void overlay(Object instance, Object overlay);

        @FieldGetter(name = "darkenScreen")
        boolean darkenScreen(Object instance);

        @FieldSetter(name = "darkenScreen", strategy = Strategy.MH)
        void darkenScreen(Object instance, boolean darkenScreen);

        @FieldGetter(name = "playMusic")
        boolean playMusic(Object instance);

        @FieldSetter(name = "playMusic", strategy = Strategy.MH)
        void playMusic(Object instance, boolean playMusic);

        @FieldGetter(name = "createWorldFog")
        boolean createWorldFog(Object instance);

        @FieldSetter(name = "createWorldFog", strategy = Strategy.MH)
        void createWorldFog(Object instance, boolean createWorldFog);
    }

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundBossEventPacket$UpdateNameOperation")
    interface UpdateNameOperationProxy {
        UpdateNameOperationProxy INSTANCE = ReflectionHelper.getProxy(UpdateNameOperationProxy.class);
        Class<?> CLAZZ = ReflectionHelper.getClass(UpdateNameOperationProxy.class);

        @ConstructorInvoker
        Object newInstance(@Type(clazz = ComponentProxy.class) Object name);
    }
}
