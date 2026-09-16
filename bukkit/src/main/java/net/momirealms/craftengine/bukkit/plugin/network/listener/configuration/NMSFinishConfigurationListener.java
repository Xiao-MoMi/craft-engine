package net.momirealms.craftengine.bukkit.plugin.network.listener.configuration;

import net.kyori.adventure.text.Component;
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

        // 暂扣配置结束包，让客户端停留在配置阶段等待资源包。
        // 当前 JoinWorldTask 已从队列移入 currentTask，必须先结束它，才能插入资源包任务。
        event.setCancelled(true);
        try {
            ServerConfigurationPacketListenerImplProxy.INSTANCE.finishCurrentTask(packetListener, JoinWorldTaskProxy.TYPE);
        } catch (Throwable e) {
            CraftEngine.instance().logger().warn("Failed to finish current task for " + user.name(), e);
            // 当前任务未成功释放时不能继续入队，否则 startNextTask 会因任务仍在运行而失败。
            user.kick(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
            return;
        }

        if (VersionHelper.isOrAbove1_20_5) {
            // 1.20.5+ 在 send 终止包时会先标记 closed，即使随后被我们取消也已生效。
            // 恢复配置阶段的保活，否则会停止正常 keepAlive，并在等待关闭超时后踢出玩家。
            ServerCommonPacketListenerImplProxy.INSTANCE.setClosed(packetListener, false);
        }

        // 链接可能异步生成；回到平台线程并确认监听器未切换后再修改配置队列。
        CraftEngine.instance().packManager().prepareResourcePacks(user).whenComplete((dataList, t) -> CraftEngine.instance().scheduler().platform().run(() -> {
            if (ConnectionProxy.INSTANCE.getPacketListener(user.connection()) != packetListener) return;
            Queue<Object> tasks = ServerConfigurationPacketListenerImplProxy.INSTANCE.getConfigurationTasks(packetListener);
            if (t != null) {
                CraftEngine.instance().logger().warn(TranslationManager.instance().plainTranslation("host.get_url_failed", user.name()), t);
                user.kick(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                return;
            }
            if (dataList.isEmpty()) {
                returnToWorld(tasks, packetListener);
                return;
            }
            user.addResourcePackTasks(dataList);
            // 最后再加入一个 JoinWorldTask 并开始资源包任务
            returnToWorld(tasks, packetListener);
        }));
    }

}
