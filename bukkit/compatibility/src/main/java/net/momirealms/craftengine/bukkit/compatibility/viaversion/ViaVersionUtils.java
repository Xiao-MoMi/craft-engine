package net.momirealms.craftengine.bukkit.compatibility.viaversion;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.bukkit.handlers.BukkitEncodeHandler;
import io.netty.channel.ChannelHandler;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.ProtocolVersion;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.field.SField;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ViaVersionUtils {
    private static SField CONNECTION_FIELD;
    private static final boolean hasBukkitEncodeHandlerClazz = ReflectionUtils.classExists("com.viaversion.viaversion.bukkit.handlers.BukkitEncodeHandler");

    private ViaVersionUtils() {}

    public static int getPlayerProtocolVersion(NetWorkUser user) {
        if (user.isFakePlayer()) { // 何意味？
            return ProtocolVersion.getByName(VersionHelper.MINECRAFT_VERSION.version()).getId();
        }
        UUID uuid = user.uuid();
        if (uuid != null) { // 不是过早获取走这里
            return Via.getAPI().getPlayerProtocolVersion(uuid).getVersion();
        }
        // 握手阶段获取走这里
        ChannelHandler viaEncoder = user.nettyChannel().pipeline().get("via-encoder");
        UserConnection connection = getUserConnection(viaEncoder);
        if (connection != null) {
            com.viaversion.viaversion.api.protocol.version.ProtocolVersion protocolInfo = connection.getProtocolInfo().protocolVersion();
            return protocolInfo != null ? protocolInfo.getVersion() : -1;
        } else {
            return -1;
        }
    }

    @Nullable
    private static UserConnection getUserConnection(ChannelHandler handler) {
        if (hasBukkitEncodeHandlerClazz && handler instanceof BukkitEncodeHandler bukkitEncodeHandler) {
            return bukkitEncodeHandler.connection();
        }
        if (CONNECTION_FIELD == null) {
            CONNECTION_FIELD = SparrowClass.of(handler.getClass()).getDeclaredSparrowField(FieldMatcher.named("connection")).mh();
        }
        return (UserConnection) CONNECTION_FIELD.get(handler);
    }
}
