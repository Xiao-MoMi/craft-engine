package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Tristate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.concurrent.CompletableFuture;

public final class PackPreferencePlayerCommand extends BukkitCommandFeature<CommandSender> {
    public static final String PACK_PERMISSION_PREFIX = "ce.resourcepack.pack.";

    public PackPreferencePlayerCommand(CraftEngineCommandManager<CommandSender> manager, CraftEngine plugin) {
        super(manager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .required("action", EnumParser.enumParser(Action.class))
                .required("pack", StringParser.stringParser(), (context, input) -> CompletableFuture.completedFuture(
                        plugin().packManager().resourcePackHosts().keySet().stream()
                                .filter(id -> context.sender().hasPermission(PACK_PERMISSION_PREFIX + id))
                                .map(Suggestion::suggestion).toList()))
                .handler(context -> {
                    String pack = context.get("pack");
                    Action action = context.get("action");
                    Player sender = context.sender();
                    if (!sender.hasPermission(PACK_PERMISSION_PREFIX + pack)) {
                        handleFeedback(context, MessageConstants.COMMAND_PACK_PREFERENCE_NO_PERMISSION, Component.text(pack));
                        return;
                    }
                    plugin().packManager().setPackPreference(sender.getUniqueId(), pack, action.state()).whenComplete((changed, error) -> {
                        if (error != null) {
                            plugin().logger().warn("Failed to save resource pack preference for " + sender.getName(), error);
                        } else if (!changed) {
                            handleFeedback(context, MessageConstants.COMMAND_PACK_PREFERENCE_UNCHANGED, Component.text(pack));
                        } else {
                            handleFeedback(context, switch (action) {
                                case ENABLE -> MessageConstants.COMMAND_PACK_PREFERENCE_ENABLED;
                                case DISABLE -> MessageConstants.COMMAND_PACK_PREFERENCE_DISABLED;
                                case RESET -> MessageConstants.COMMAND_PACK_PREFERENCE_RESET;
                            }, Component.text(pack));
                        }
                    });
                });
    }

    @Override
    public String getFeatureID() {
        return "pack_preference_player";
    }

    public enum Action {
        ENABLE,
        DISABLE,
        RESET;

        public Tristate state() {
            return switch (this) {
                case ENABLE -> Tristate.TRUE;
                case DISABLE -> Tristate.FALSE;
                case RESET -> Tristate.UNDEFINED;
            };
        }
    }
}
