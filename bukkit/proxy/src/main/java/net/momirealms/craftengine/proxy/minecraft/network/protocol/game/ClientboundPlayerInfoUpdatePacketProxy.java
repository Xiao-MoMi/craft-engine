package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import com.mojang.authlib.GameProfile;
import net.momirealms.craftengine.proxy.minecraft.network.FriendlyByteBufProxy;
import net.momirealms.craftengine.proxy.minecraft.network.RegistryFriendlyByteBufProxy;
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

        @FieldGetter(name = "profileId")
        UUID getProfileId(Object target);
    }

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action")
    interface ActionProxy {
        ActionProxy INSTANCE = ASMProxyFactory.create(ActionProxy.class);
        Class<?> CLASS = SparrowClass.find("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
        Enum<?>[] VALUES = INSTANCE.values();
        Enum<?> ADD_PLAYER = VALUES[0];
        Enum<?> INITIALIZE_CHAT = VALUES[1];
        Enum<?> UPDATE_GAME_MODE = VALUES[2];
        Enum<?> UPDATE_LISTED = VALUES[3];
        Enum<?> UPDATE_LATENCY = VALUES[4];
        Enum<?> UPDATE_DISPLAY_NAME = VALUES[5];

        @MethodInvoker(name = "values", isStatic = true)
        Enum<?>[] values();

        @FieldGetter(name = "reader")
        Object getReader(Object target);

        @FieldGetter(name = "writer")
        Object getWriter(Object target);

        @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action$Reader")
        interface ReaderProxy {
            ReaderProxy INSTANCE = ASMProxyFactory.create(ReaderProxy.class);

            @MethodInvoker(name = "read", activeIf = "max_version=1.20.4")
            void read$0(Object target, @Type(clazz = EntryBuilderProxy.class) Object builder, @Type(clazz = FriendlyByteBufProxy.class) Object buf);

            @MethodInvoker(name = "read", activeIf = "min_version=1.20.5")
            void read$1(Object target, @Type(clazz = EntryBuilderProxy.class) Object builder, @Type(clazz = RegistryFriendlyByteBufProxy.class) Object buf);
        }

        @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action$Writer")
        interface WriterProxy {
            WriterProxy INSTANCE = ASMProxyFactory.create(WriterProxy.class);

            @MethodInvoker(name = "write", activeIf = "max_version=1.20.4")
            void write$0(Object target, @Type(clazz = FriendlyByteBufProxy.class) Object buf, @Type(clazz = EntryProxy.class) Object entry);

            @MethodInvoker(name = "write", activeIf = "min_version=1.20.5")
            void write$1(Object target, @Type(clazz = RegistryFriendlyByteBufProxy.class) Object buf, @Type(clazz = EntryProxy.class) Object entry);
        }
    }

    @ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$EntryBuilder")
    interface EntryBuilderProxy {
        EntryBuilderProxy INSTANCE = ASMProxyFactory.create(EntryBuilderProxy.class);

        @ConstructorInvoker
        Object newInstance(UUID profileId);

        @MethodInvoker(name = "build")
        Object build(Object target);
    }
}
