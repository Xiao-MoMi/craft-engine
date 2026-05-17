package net.momirealms.craftengine.bukkit.plugin.network.listener.common;

import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.pack.host.ResourcePackResponseAction;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.Debugger;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.NMSPacketListener;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.ConnectionProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ServerCommonPacketListenerProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.common.ServerboundResourcePackPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerConfigurationPacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.config.ServerResourcePackConfigurationTaskProxy;

import java.util.UUID;

public final class NMSResourcePackListener implements NMSPacketListener {
    public static final NMSPacketListener INSTANCE = new NMSResourcePackListener();

    private NMSResourcePackListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (VersionHelper.isOrAbove1_20_3) {
            UUID uuid = ServerboundResourcePackPacketProxy.INSTANCE.getId(packet);
            if (!user.isResourcePackLoading(uuid)) {
                // 不是CraftEngine发送的资源包,不管
                return;
            }
        }

        ResourcePackResponseAction action = ResourcePackResponseAction.byOrdinal(ServerboundResourcePackPacketProxy.INSTANCE.getAction(packet).ordinal());

        ResourcePackHost host = CraftEngine.instance().packManager().resourcePackHost();
        host.response(user, action).whenComplete((returnAction, t) -> {
            if (t != null) {
                CraftEngine.instance().logger().warn(TranslationManager.instance().plainTranslation("host.handle_response_failed", user.name()), t);
                return;
            }
            // 响应不是最终的或者没有配置阶段就不处理后续
            if (returnAction.intermediate() || !VersionHelper.isOrAbove1_20_2) return;
            event.setCancelled(true);
            Object packetListener = ConnectionProxy.INSTANCE.getPacketListener(user.connection());
            if (!ServerConfigurationPacketListenerImplProxy.CLASS.isInstance(packetListener)) return; // 不是配置阶段不处理
            // 主线程上处理这个包
            CraftEngine.instance().scheduler().platform().run(() -> {
                try {
                    // 当客户端发出多次成功包的时候，finish会报错，我们忽略他
                    ServerCommonPacketListenerProxy.INSTANCE.handleResourcePackResponse(packetListener, packet);
                    if (VersionHelper.isPaper && VersionHelper.isOrAbove1_21_7) { // paper在1.21.7+增加了判断不会主动结束任务
                        ServerConfigurationPacketListenerImplProxy.INSTANCE.finishCurrentTask(packetListener, ServerResourcePackConfigurationTaskProxy.TYPE);
                    }
                } catch (Throwable e) {
                    Debugger.RESOURCE_PACK.warn(() -> "Cannot finish current task", e);
                }
            });
        });
    }
}
