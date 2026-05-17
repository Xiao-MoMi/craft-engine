package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;

public final class DebugClearCooldownCommand extends BukkitCommandFeature<CommandSender> {

    public DebugClearCooldownCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("player", SinglePlayerSelectorParser.singlePlayerSelectorParser())
                .handler(context -> {
                    SinglePlayerSelector playerSelector = context.get("player");
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(playerSelector.single());
                    if (serverPlayer == null) return;
                    serverPlayer.cooldown().clearCooldowns();
                    plugin().senderFactory().wrap(context.sender()).sendMessage(Component.text("Done clearing cooldowns!"));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_clear_cooldown";
    }
}
