package net.momirealms.craftengine.proxy.minecraft.network.protocol.game;

import com.mojang.authlib.GameProfile;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.RemoteChatSessionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.GameTypeProxy;
import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import javax.annotation.Nullable;
import java.util.UUID;

@ReflectionProxy(name = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket")
public interface ClientboundPlayerInfoUpdatePacketProxy {
    ClientboundPlayerInfoUpdatePacketProxy INSTANCE = ASMProxyFactory.create(ClientboundPlayerInfoUpdatePacketProxy.class);

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
    }
}
