package net.momirealms.craftengine.bukkit.reflection.minecraft.network.protocol.game;

import net.momirealms.craftengine.bukkit.reflection.ReflectionHelper;
import net.momirealms.craftengine.bukkit.reflection.adventure.text.AdventureComponentProxy;
import net.momirealms.craftengine.bukkit.reflection.minecraft.network.chat.ComponentProxy;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.jetbrains.annotations.Nullable;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundSystemChatPacket")
public interface ClientboundSystemChatPacketProxy {
    ClientboundSystemChatPacketProxy INSTANCE = ReflectionHelper.getProxy(ClientboundSystemChatPacketProxy.class);
    Class<?> CLAZZ = ReflectionHelper.getClass(ClientboundSystemChatPacketProxy.class);

    @ConstructorInvoker
    Object newInstance(@Type(clazz = ComponentProxy.class) Object content, boolean overlay);

    @ConstructorInvoker(version = "<1.20.3")
    Object newInstance(@Nullable @Type(clazz = AdventureComponentProxy.class) Object adventure$content, @Nullable String content, boolean overlay);

    @FieldGetter(name = "overlay")
    boolean overlay(Object instance);

    @FieldSetter(name = "overlay", strategy = Strategy.MH)
    void overlay(Object instance, boolean overlay);

    @FieldGetter(name = "content", version = ">=1.20.3")
    Object content(Object instance);

    @FieldSetter(name = "content", strategy = Strategy.MH, version = ">=1.20.3")
    void content(Object instance, Object content);

    @FieldGetter(name = "adventure$content", version = "<1.20.3")
    Object adventure$content(Object instance);

    @FieldSetter(name = "adventure$content", strategy = Strategy.MH, version = "<1.20.3")
    void adventure$content(Object instance, Object component);

    @FieldGetter(name = "content", version = "<1.20.3")
    String string$content(Object instance);

    @FieldSetter(name = "content", strategy = Strategy.MH, version = "<1.20.3")
    void string$content(Object instance, String content);
}
