package net.momirealms.craftengine.bukkit.compatibility.viaversion;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.rewriter.ItemRewriter;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.bukkit.handlers.BukkitEncodeHandler;
import io.netty.channel.ChannelHandler;
import net.momirealms.craftengine.core.item.network.ClientboundItemRewriter;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.ProtocolVersion;
import net.momirealms.craftengine.core.util.ReflectionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.field.SField;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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

    @SuppressWarnings("rawtypes")
    @Nullable
    public static ClientboundItemRewriter getClientboundItemRewriter(NetWorkUser user, int serverProtocol, int clientProtocol) {
        UserConnection connection = getMatchingServerConnection(user, serverProtocol, clientProtocol);
        if (connection == null) {
            return null;
        }

        List<Protocol> protocols = connection.getProtocolInfo().getPipeline().reversedPipes();
        List<ItemRewriter<?>> rewriters = new ArrayList<>(protocols.size());
        for (Protocol<?, ?, ?, ?> protocol : protocols) {
            ItemRewriter<?> rewriter = protocol.getItemRewriter();
            if (rewriter != null) {
                rewriters.add(rewriter);
            }
        }
        if (rewriters.isEmpty()) {
            return null;
        }
        Type<Item> sourceType = rewriters.getFirst().itemType();
        Type<Item> targetType = rewriters.getLast().mappedItemType();
        if (sourceType == null || targetType == null) {
            return null;
        }
        return (source, target) -> {
            Item item = sourceType.read(source);
            if (source.isReadable()) {
                throw new IllegalArgumentException("ViaVersion did not consume the complete creative item stack");
            }
            for (int i = 0, size = rewriters.size(); i < size; i++) {
                item = rewriters.get(i).handleItemToClient(connection, item);
            }
            if (!Item.isEmpty(item)) {
                targetType.write(target, item);
            }
        };
    }

    @Nullable
    private static UserConnection getMatchingServerConnection(NetWorkUser user, int serverProtocol, int clientProtocol) {
        UUID uuid = user.uuid();
        if (!Via.isLoaded() || uuid == null) {
            return null;
        }
        UserConnection connection = Via.getManager().getConnectionManager().getServerConnection(uuid);
        if (connection == null || !connection.isActive() || connection.getChannel() != user.nettyChannel()) {
            return null;
        }
        ProtocolInfo info = connection.getProtocolInfo();
        if (info.serverProtocolVersion().getVersion() != serverProtocol || info.protocolVersion().getVersion() != clientProtocol) {
            return null;
        }
        return connection;
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
