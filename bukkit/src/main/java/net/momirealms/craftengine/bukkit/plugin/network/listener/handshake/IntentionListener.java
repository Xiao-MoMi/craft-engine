package net.momirealms.craftengine.bukkit.plugin.network.listener.handshake;

import net.momirealms.craftengine.bukkit.plugin.network.BukkitNetworkManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.ConnectionState;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.ProtocolVersion;
import net.momirealms.craftengine.core.plugin.network.event.ByteBufPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.ByteBufferPacketListener;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.bukkit.Bukkit;

public final class IntentionListener implements ByteBufferPacketListener {
    public static final ByteBufferPacketListener INSTANCE = new IntentionListener();
    private final boolean hasViaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion") != null;

    private IntentionListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, ByteBufPacketEvent event) {
        FriendlyByteBuf buf = event.getBuffer();
        int protocolVersion;
        ConnectionState nextState;
        try {
            protocolVersion = buf.readVarInt();
            buf.readUtf(); // serverAddress
            buf.readUnsignedShort(); // serverPort
            nextState = switch (buf.readVarInt()) {
                case 1 -> ConnectionState.STATUS;
                case 2, 3 -> ConnectionState.LOGIN;
                default -> null;
            };
        } catch (Throwable e) { // 客户端乱发包
            Debugger.COMMON.warn(() -> "Failed to read intention packet", e);
            user.kick(null);
            return;
        }
        if (nextState == null) { // 如果乱发包直接强行断开连接
            user.kick(null);
            return;
        }
        if (nextState == ConnectionState.LOGIN) { // 重定位一下 channel handler，确保在pe后处理
            BukkitNetworkManager.relocateChannelHandler(user.nettyChannel());
        }
        if (this.hasViaVersion) {
            int viaVersionProtocolVersion = CraftEngine.instance().compatibilityManager().getViaVersionProtocolVersion(user);
            if (viaVersionProtocolVersion != -1) {
                protocolVersion = viaVersionProtocolVersion;
            }
        }
        user.setProtocolVersion(ProtocolVersion.getById(protocolVersion));
        /*
        1.20+ 直接切换
         */
        user.setConnectionState(nextState);
    }
}
