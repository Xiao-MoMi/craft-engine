package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;

public final class UnsetLocaleCommand extends BukkitCommandFeature<CommandSender> {

    public UnsetLocaleCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .required("player", SinglePlayerSelectorParser.singlePlayerSelectorParser())
                .handler(context -> {
                    SinglePlayerSelector playerSelector = context.get("player");
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(playerSelector.single());
                    if (serverPlayer == null) return;
                    serverPlayer.setSelectedLocale(null);
                    handleFeedback(context, MessageConstants.COMMAND_LOCALE_UNSET_SUCCESS, Component.text(serverPlayer.name()));
                });
    }

    @Override
    public String getFeatureID() {
        return "unset_locale";
    }
}
