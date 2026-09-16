package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class DebugPackStatesCommand extends BukkitCommandFeature<CommandSender> {

    public DebugPackStatesCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .optional("player", SinglePlayerSelectorParser.singlePlayerSelectorParser())
                .handler(context -> {
                    Sender sender = plugin().senderFactory().wrap(context.sender());
                    SinglePlayerSelector selector = context.getOrDefault("player", null);
                    Player target;
                    if (selector != null) {
                        target = selector.single();
                    } else if (context.sender() instanceof Player self) {
                        target = self;
                    } else {
                        sender.sendMessage(DebugCommandOutput.error("A player is required from the console"));
                        return;
                    }
                    BukkitServerPlayer player = BukkitAdaptor.adapt(target);
                    if (player == null) return;
                    sender.sendMessage(DebugCommandOutput.title("Pack States"));
                    sender.sendMessage(DebugCommandOutput.value("Player", player.name()));
                    Map<String, Boolean> states = plugin().packManager().packPreferences(player);
                    if (states == null) {
                        sender.sendMessage(DebugCommandOutput.warning("Pack states have not loaded yet"));
                        return;
                    }
                    Set<String> configured = plugin().packManager().resourcePackHosts().keySet();
                    // 展示所有已保存的偏好，不能按当前服务器配置过滤；本服未设置偏好的包仍显示 default。
                    Set<String> packs = new TreeSet<>(states.keySet());
                    packs.addAll(configured);
                    if (packs.isEmpty()) {
                        sender.sendMessage(DebugCommandOutput.empty(1));
                        return;
                    }
                    for (String pack : packs) {
                        Boolean enabled = states.get(pack);
                        Component state = enabled == null
                                ? Component.text("unset", NamedTextColor.YELLOW)
                                : Component.text(enabled ? "enabled" : "disabled", enabled ? NamedTextColor.GREEN : NamedTextColor.RED);
                        // 删除线只标记本服不存在的包名，仍按原始偏好展示其启用或禁用状态。
                        Component name = Component.text(pack, NamedTextColor.GRAY)
                                .decoration(TextDecoration.STRIKETHROUGH, !configured.contains(pack));
                        sender.sendMessage(DebugCommandOutput.value(1, name, state));
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_pack_states";
    }
}
