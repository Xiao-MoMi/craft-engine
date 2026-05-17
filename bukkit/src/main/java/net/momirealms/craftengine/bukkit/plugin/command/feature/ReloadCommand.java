package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Timestamp;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.EnumParser;

import java.util.Optional;

public final class ReloadCommand extends BukkitCommandFeature<CommandSender> {
    public static boolean RELOAD_PACK_FLAG = false;

    public ReloadCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(manager.flagBuilder("silent").withAliases("s"))
                .optional("content", EnumParser.enumParser(ReloadArgument.class))
                .handler(context -> {
                    if (plugin().isReloading()) {
                        handleFeedback(context, MessageConstants.COMMAND_RELOAD_TOO_FAST);
                        return;
                    }
                    Optional<ReloadArgument> optional = context.optional("content");
                    ReloadArgument argument = ReloadArgument.CONFIG;
                    if (optional.isPresent()) {
                        argument = optional.get();
                    }
                    if (argument == ReloadArgument.CONFIG || argument == ReloadArgument.RECIPE) {
                        plugin().reloadPlugin(plugin().scheduler().async(), r -> plugin().scheduler().platform().run(r), argument == ReloadArgument.RECIPE).thenAccept(reloadResult -> {
                            if (reloadResult.success()) {
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_SUCCESS,
                                        Component.text(reloadResult.asyncTime() + reloadResult.syncTime()),
                                        Component.text(reloadResult.asyncTime()),
                                        Component.text(reloadResult.syncTime())
                                );
                                if (reloadResult.issues() != 0 && context.sender() instanceof Player) {
                                    handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_ISSUES, Component.text(reloadResult.issues()));
                                }
                            } else {
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_FAILURE);
                            }
                        });
                    } else if (argument == ReloadArgument.PACK) {
                        plugin().scheduler().executeAsync(() -> {
                            try {
                                Timestamp timestamp = new Timestamp();
                                plugin().packManager().generateResourcePack();
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_PACK_SUCCESS, Component.text(timestamp.deltaMillis()));
                            } catch (Throwable e) {
                                plugin().logger().warn("Failed to generate resource pack", e);
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_PACK_FAILURE);
                            }
                        });
                    } else if (argument == ReloadArgument.ALL) {
                        RELOAD_PACK_FLAG = true;
                        plugin().reloadPlugin(plugin().scheduler().async(), r -> plugin().scheduler().platform().run(r), true).thenAcceptAsync(reloadResult -> {
                            if (reloadResult.success()) {
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_SUCCESS,
                                        Component.text(reloadResult.asyncTime() + reloadResult.syncTime()),
                                        Component.text(reloadResult.asyncTime()),
                                        Component.text(reloadResult.syncTime())
                                );
                                if (reloadResult.issues() != 0 && context.sender() instanceof Player) {
                                    handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_ISSUES, Component.text(reloadResult.issues()));
                                }
                                try {
                                    Timestamp timestamp = new Timestamp();
                                    plugin().packManager().generateResourcePack();
                                    handleFeedback(context, MessageConstants.COMMAND_RELOAD_PACK_SUCCESS, Component.text(timestamp.deltaMillis()));
                                } catch (Throwable e) {
                                    plugin().logger().warn("Failed to generate resource pack", e);
                                    handleFeedback(context, MessageConstants.COMMAND_RELOAD_PACK_FAILURE);
                                } finally {
                                    RELOAD_PACK_FLAG = false;
                                }
                            } else {
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_FAILURE);
                                RELOAD_PACK_FLAG = false;
                            }
                        }, plugin().scheduler().async());
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "reload";
    }

    public enum ReloadArgument {
        CONFIG,
        RECIPE,
        PACK,
        ALL
    }
}
