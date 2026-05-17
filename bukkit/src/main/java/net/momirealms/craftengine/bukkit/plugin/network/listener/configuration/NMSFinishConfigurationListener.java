package net.momirealms.craftengine.bukkit.plugin.network.listener.configuration;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.util.ResourcePackUtils;
import net.momirealms.craftengine.core.pack.host.ResourcePackDownloadData;
import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.plugin.network.event.NMSPacketEvent;
import net.momirealms.craftengine.core.plugin.network.listener.NMSPacketListener;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.network.ConnectionProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerCommonPacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.ServerConfigurationPacketListenerImplProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.config.JoinWorldTaskProxy;
import net.momirealms.craftengine.proxy.minecraft.server.network.config.ServerResourcePackConfigurationTaskProxy;

import java.util.Queue;

public final class NMSFinishConfigurationListener implements NMSPacketListener {
    public static final NMSPacketListener INSTANCE = VersionHelper.isOrAbove1_20_2 ? new NMSFinishConfigurationListener() : null;

    private NMSFinishConfigurationListener() {}

    private static void returnToWorld(Queue<Object> tasks, Object packetListener) {
        tasks.add(JoinWorldTaskProxy.INSTANCE.newInstance());
        ServerConfigurationPacketListenerImplProxy.INSTANCE.startNextTask(packetListener);
    }

    @Override
    public void onPacketSend(NetWorkUser user, NMSPacketEvent event, Object packet) {
        if (!Config.sendPackOnJoin()) {
            // 防止后期调试进配置阶段造成问题
            user.setShouldProcessFinishConfiguration(false);
            return;
        }

        if (!user.shouldProcessFinishConfiguration()) {
            return;
        }
        Object packetListener = ConnectionProxy.INSTANCE.getPacketListener(user.connection());
        if (!ServerConfigurationPacketListenerImplProxy.CLASS.isInstance(packetListener)) {
            return;
        }

        // 防止后续加入的JoinWorldTask再次处理
        user.setShouldProcessFinishConfiguration(false);

        // 检查用户UUID是否已经校验
        if (!user.isUUIDVerified()) {
            if (Config.strictPlayerUuidValidation()) {
                user.kick(Component.translatable("disconnect.loginFailedInfo").arguments(Component.translatable("argument.uuid.invalid")));
                return;
            }
        }

        // 取消 ClientboundFinishConfigurationPacket，让客户端发呆，并结束掉当前的进入世界任务
        event.setCancelled(true);
        try {
            ServerConfigurationPacketListenerImplProxy.INSTANCE.finishCurrentTask(packetListener, JoinWorldTaskProxy.TYPE);
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Failed to finish current task for " + user.name(), e);
        }

        if (VersionHelper.isOrAbove1_20_5) {
            // 1.20.5+开始会检查是否结束需要重新设置回去，不然不会发keepAlive包
            ServerCommonPacketListenerImplProxy.INSTANCE.setClosed(packetListener, false);
        }

        // 请求资源包
        ResourcePackHost host = CraftEngine.instance().packManager().resourcePackHost();
        host.requestResourcePackDownloadLink(user).whenComplete((dataList, t) -> {
            Queue<Object> tasks = ServerConfigurationPacketListenerImplProxy.INSTANCE.getConfigurationTasks(packetListener);
            if (t != null) {
                CraftEngine.instance().logger().warn(TranslationManager.instance().plainTranslation("host.get_url_failed", user.name()), t);
                returnToWorld(tasks, packetListener);
                return;
            }
            if (dataList.isEmpty()) {
                returnToWorld(tasks, packetListener);
                return;
            }
            // 向配置阶段连接的任务重加入资源包的任务
            if (VersionHelper.isOrAbove1_20_3) {
                for (ResourcePackDownloadData data : dataList) {
                    tasks.add(ServerResourcePackConfigurationTaskProxy.INSTANCE.newInstance(ResourcePackUtils.createServerResourcePackInfo(data.uuid(), data.url(), data.sha1())));
                    user.addResourcePackUUID(data.uuid());
                }
            } else { // 1.20.2 只支持一个服务器资源包
                ResourcePackDownloadData data = dataList.getFirst();
                tasks.add(ServerResourcePackConfigurationTaskProxy.INSTANCE.newInstance(ResourcePackUtils.createServerResourcePackInfo(data.uuid(), data.url(), data.sha1())));
                user.addResourcePackUUID(data.uuid());
            }
            // 最后再加入一个 JoinWorldTask 并开始资源包任务
            returnToWorld(tasks, packetListener);
        });
    }

}
