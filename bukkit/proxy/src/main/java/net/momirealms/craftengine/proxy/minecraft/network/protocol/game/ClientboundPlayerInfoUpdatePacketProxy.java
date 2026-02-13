package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import com.mojang.authlib.GameProfile;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.RemoteChatSessionProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.PacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.GameTypeProxy;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket")
public interface ClientboundPlayerInfoUpdatePacketProxy extends PacketProxy {
    ClientboundPlayerInfoUpdatePacketProxy INSTANCE = ASMProxyFactory.create(ClientboundPlayerInfoUpdatePacketProxy.class);
    Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");

    @ConstructorInvoker
    Object newInstance(EnumSet<?> actions, List<?> entries);

    @FieldGetter(name = "actions")
    EnumSet<? extends Enum<?>> getActions(Object target);

    @FieldGetter(name = "entries")
    List<Object> getEntries(Object target);

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry")
    interface EntryProxy {
        EntryProxy INSTANCE = ASMProxyFactory.create(EntryProxy.class);

        @ConstructorInvoker(activeIf = "min_version=1.21.4")
        Object newInstance(
                UUID profileId,
                @Nullable GameProfile profile,
                boolean listed,
                int latency,
                @Type(clazz = GameTypeProxy.class) Object gameMode,
                @Nullable @Type(clazz = ComponentProxy.class) Object displayName,
                boolean showHat,
                int listOrder,
                @Nullable @Type(clazz = RemoteChatSessionProxy.DataProxy.class) Object chatSession
        );

        @ConstructorInvoker(activeIf = "min_version=1.21.2 && max_version=1.21.3")
        Object newInstance(
                UUID profileId,
                @Nullable GameProfile profile,
                boolean listed,
                int latency,
                @Type(clazz = GameTypeProxy.class) Object gameMode,
                @Nullable @Type(clazz = ComponentProxy.class) Object displayName,
                int listOrder,
                @Nullable @Type(clazz = RemoteChatSessionProxy.DataProxy.class) Object chatSession
        );

        @ConstructorInvoker(activeIf = "max_version=1.21.1")
        Object newInstance(
                UUID profileId,
                @Nullable GameProfile profile,
                boolean listed,
                int latency,
                @Type(clazz = GameTypeProxy.class) Object gameMode,
                @Nullable @Type(clazz = ComponentProxy.class) Object displayName,
                @Nullable @Type(clazz = RemoteChatSessionProxy.DataProxy.class) Object chatSession
        );

        @FieldGetter(name = "displayName")
        Object getDisplayName(Object target);

        @FieldSetter(name = "displayName")
        void setDisplayName(Object target, Object displayName);
    }

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action")
    interface ActionProxy {
        ActionProxy INSTANCE = ASMProxyFactory.create(ActionProxy.class);
        Enum<?>[] VALUES = INSTANCE.values();
        Enum<?> ADD_PLAYER = VALUES[0];
        Enum<?> INITIALIZE_CHAT = VALUES[1];
        Enum<?> UPDATE_GAME_MODE = VALUES[2];
        Enum<?> UPDATE_LISTED = VALUES[3];
        Enum<?> UPDATE_LATENCY = VALUES[4];
        Enum<?> UPDATE_DISPLAY_NAME = VALUES[5];

        @MethodInvoker(name = "values", isStatic = true)
        Enum<?>[] values();
    }
}
