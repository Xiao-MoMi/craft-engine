package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;
import org.incendo.cloud.parser.standard.BooleanParser;

import java.util.Optional;

public final class ToggleEntityCullingCommand extends BukkitCommandFeature<CommandSender> {

    public ToggleEntityCullingCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .required("player", SinglePlayerSelectorParser.singlePlayerSelectorParser())
                .optional("state", BooleanParser.booleanParser())
                .handler(context -> {
                    if (!Config.enableEntityCulling()) {
                        plugin().senderFactory().wrap(context.sender()).sendMessage(Component.text("Entity culling is not enabled on this server").color(NamedTextColor.RED));
                        return;
                    }
                    SinglePlayerSelector playerSelector = context.get("player");
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(playerSelector.single());
                    if (serverPlayer == null) return;
                    Optional<Boolean> state = context.optional("state");
                    boolean isEnabled = serverPlayer.enableEntityCulling();
                    if (state.isPresent()) {
                        serverPlayer.setEnableEntityCulling(state.get());
                        handleFeedback(context, MessageConstants.COMMAND_TOGGLE_ENTITY_CULLING_SUCCESS, Component.text(state.get()), Component.text(serverPlayer.name()));
                    } else {
                        serverPlayer.setEnableEntityCulling(!isEnabled);
                        handleFeedback(context, MessageConstants.COMMAND_TOGGLE_ENTITY_CULLING_SUCCESS, Component.text(!isEnabled), Component.text(serverPlayer.name()));
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "toggle_entity_culling";
    }
}
