package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.data.MultiplePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.MultiplePlayerSelectorParser;

import java.util.Collection;

public final class SendResourcePackCommand extends BukkitCommandFeature<CommandSender> {

    public SendResourcePackCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .required("player", MultiplePlayerSelectorParser.multiplePlayerSelectorParser(false))
                .handler(context -> {
                    MultiplePlayerSelector selector = context.get("player");
                    Collection<Player> players = selector.values();
                    for (Player player : players) {
                        BukkitServerPlayer bukkitServerPlayer = BukkitAdaptor.adapt(player);
                        if (bukkitServerPlayer == null) continue;
                        BukkitCraftEngine.instance().packManager().sendResourcePack(bukkitServerPlayer);
                    }
                    int size = players.size();
                    if (size == 1) {
                        String name = players.iterator().next().getName();
                        handleFeedback(context, MessageConstants.COMMAND_SEND_RESOURCE_PACK_SUCCESS_SINGLE, Component.text(name));
                    } else {
                        handleFeedback(context, MessageConstants.COMMAND_SEND_RESOURCE_PACK_SUCCESS_MULTIPLE, Component.text(size));
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "send_resource_pack";
    }
}
