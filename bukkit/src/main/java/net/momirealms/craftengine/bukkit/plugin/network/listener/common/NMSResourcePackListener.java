package net.momirealms.craftengine.bukkit.plugin.network.listener.common;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.pack.ResourcePackConfigurationTask;
import net.momirealms.craftengine.core.pack.host.ResourcePackResponseAction;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
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
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.UUID;

public final class NMSResourcePackListener implements NMSPacketListener {
    public static final NMSPacketListener INSTANCE = new NMSResourcePackListener();

    private NMSResourcePackListener() {}

    @Override
    public void onPacketReceive(NetWorkUser user, NMSPacketEvent event, Object packet) {
        UUID packId = null;
        if (VersionHelper.isOrAbove1_20_3) {
            UUID uuid = ServerboundResourcePackPacketProxy.INSTANCE.getId(packet);
            if (!user.isResourcePackLoading(uuid)) {
                // 不是CraftEngine发送的资源包,不管
                return;
            }
            packId = uuid;
        }

        ResourcePackResponseAction action = ResourcePackResponseAction.byOrdinal(ServerboundResourcePackPacketProxy.INSTANCE.getAction(packet).ordinal());
        Object configurationListener = VersionHelper.isOrAbove1_20_2 ? ConnectionProxy.INSTANCE.getPacketListener(user.connection()) : null;
        // 1.20/1.20.1 没有配置监听器类，先短路判断，避免访问不存在的 CLASS。
        Object configurationTask = configurationListener != null && ServerConfigurationPacketListenerImplProxy.CLASS.isInstance(configurationListener) ? ServerConfigurationPacketListenerImplProxy.INSTANCE.getCurrentTask(configurationListener) : null;
        ResourcePackConfigurationTask batch = ResourcePackConfigurationTask.from(configurationTask);
        if (batch != null) {
            // 必须在调度到平台线程之前取消原包，防止原版配置监听器按单包终态结束整个任务。
            event.setCancelled(true);
            if (!batch.isPending(packId)) return;
            UUID responsePackId = packId;
            // 父类响应处理要求服务端线程；配置队列的推进也统一放到平台线程执行。
            CraftEngine.instance().scheduler().platform().run(() -> {
                // 等待平台线程执行期间可能已切换监听器或配置任务，旧响应不能推进新任务。
                if (ConnectionProxy.INSTANCE.getPacketListener(user.connection()) != configurationListener || ServerConfigurationPacketListenerImplProxy.INSTANCE.getCurrentTask(configurationListener) != configurationTask || !batch.isPending(responsePackId)) return;
                try {
                    ResourcePackConfigurationTask.handleResponse(configurationListener, packet);
                    // 父类不更新 Paper 的连接状态，原本由配置监听器的覆写负责，需在此补齐。
                    if (VersionHelper.hasPaperPatch && action != ResourcePackResponseAction.UNKNOWN) {
                        ConnectionProxy.INSTANCE.setResourcePackStatus(user.connection(), PlayerResourcePackStatusEvent.Status.valueOf(action.name()));
                    }
                    // ACCEPTED 和 DOWNLOADED 都是中间态，必须继续等待终态。
                    if (action.intermediate()) return;
                    // 拒绝和失败是否断开连接由配置决定；未知响应不放行。
                    if (action == ResourcePackResponseAction.UNKNOWN || action.shouldDisconnect(Config.kickOnDeclined(), Config.kickOnFailedApply())) {
                        batch.fail();
                        user.kick(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                        return;
                    }
                    // 成功或配置允许的拒绝/失败都结束当前包的等待，整批结束后才放行。
                    if (batch.markCompleted(responsePackId)) {
                        // finishCurrentTask 自带 startNextTask，只调用一次；队尾 JoinWorldTask 随后放行。
                        ServerConfigurationPacketListenerImplProxy.INSTANCE.finishCurrentTask(configurationListener, ServerResourcePackConfigurationTaskProxy.TYPE);
                    }
                } catch (Throwable e) {
                    batch.fail();
                    CraftEngine.instance().logger().warn("Failed to handle resource pack batch response for " + user.name(), e);
                    user.kick(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                }
            });
            return;
        }

        // 非批量任务沿用原有踢出配置；客户端响应策略与资源包托管方式无关。
        boolean disconnect = action.shouldDisconnect(Config.kickOnDeclined(), Config.kickOnFailedApply());
        if (disconnect) {
            user.kick(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
            return;
        }
        // 中间态及游玩阶段的响应交给服务端处理，保留原生状态更新和事件。
        if (action.intermediate() || !VersionHelper.isOrAbove1_20_2) return;
        if (!ServerConfigurationPacketListenerImplProxy.CLASS.isInstance(configurationListener)) return;
        event.setCancelled(true);
        CraftEngine.instance().scheduler().platform().run(() -> {
            try {
                // 当客户端发出多次成功包的时候，finish会报错，我们忽略他
                ServerCommonPacketListenerProxy.INSTANCE.handleResourcePackResponse(configurationListener, packet);
                if (VersionHelper.hasPaperPatch && VersionHelper.isOrAbove1_21_7) { // paper在1.21.7+增加了判断不会主动结束任务
                    ServerConfigurationPacketListenerImplProxy.INSTANCE.finishCurrentTask(configurationListener, ServerResourcePackConfigurationTaskProxy.TYPE);
                }
            } catch (Throwable e) {
                Debugger.RESOURCE_PACK.warn(() -> "Cannot finish current task", e);
            }
        });
    }
}
