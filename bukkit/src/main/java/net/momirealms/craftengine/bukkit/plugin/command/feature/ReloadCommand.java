package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.EnumParser;

import java.util.Optional;

public class ReloadCommand extends BukkitCommandFeature<CommandSender> {
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
                        handleFeedback(context, MessageConstants.COMMAND_RELOAD_FAILURE_IS_LOADING);
                        return;
                    }
                    Optional<ReloadArgument> optional = context.optional("content");
                    ReloadArgument argument = ReloadArgument.CONFIG;
                    if (optional.isPresent()) {
                        argument = optional.get();
                    }
                    if (argument == ReloadArgument.CONFIG) {
                        try {
                            plugin().reloadPlugin(plugin().scheduler().async(), r -> plugin().scheduler().sync().run(r), false).thenAccept(reloadResult -> {
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_SUCCESS,
                                        Component.text(reloadResult.asyncTime() + reloadResult.syncTime()),
                                        Component.text(reloadResult.asyncTime()),
                                        Component.text(reloadResult.syncTime())
                                );
                            });
                        } catch (Exception e) {
                            handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_FAILURE);
                            plugin().logger().warn("Failed to reload config", e);
                        }
                    } else if (argument == ReloadArgument.RECIPE) {
                        try {
                            plugin().reloadPlugin(plugin().scheduler().async(), r -> plugin().scheduler().sync().run(r), true).thenAccept(reloadResult -> {
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_SUCCESS,
                                        Component.text(reloadResult.asyncTime() + reloadResult.syncTime()),
                                        Component.text(reloadResult.asyncTime()),
                                        Component.text(reloadResult.syncTime())
                                );
                            });
                        } catch (Exception e) {
                            handleFeedback(context, MessageConstants.COMMAND_RELOAD_CONFIG_FAILURE);
                            plugin().logger().warn("Failed to reload config", e);
                        }
                    } else if (argument == ReloadArgument.PACK) {
                        plugin().scheduler().executeAsync(() -> {
                            try {
                                long time1 = System.currentTimeMillis();
                                plugin().packManager().generateResourcePack();
                                long time2 = System.currentTimeMillis();
                                long packTime = time2 - time1;
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_PACK_SUCCESS, Component.text(packTime));
                            } catch (Exception e) {
                                handleFeedback(context, MessageConstants.COMMAND_RELOAD_PACK_FAILURE);
                                plugin().logger().warn("Failed to generate resource pack", e);
                            }
                        });
                    } else if (argument == ReloadArgument.ALL) {
                        RELOAD_PACK_FLAG = true;
                        try {
                            plugin().reloadPlugin(plugin().scheduler().async(), r -> plugin().scheduler().sync().run(r), !VersionHelper.isFolia()).thenAcceptAsync(reloadResult -> {
                                try {
                                    long time1 = System.currentTimeMillis();
                                    plugin().packManager().generateResourcePack();
                                    long time2 = System.currentTimeMillis();
                                    long packTime = time2 - time1;
                                    handleFeedback(context, MessageConstants.COMMAND_RELOAD_ALL_SUCCESS,
                                            Component.text(reloadResult.asyncTime() + reloadResult.syncTime() + packTime),
                                            Component.text(reloadResult.asyncTime()),
                                            Component.text(reloadResult.syncTime()),
                                            Component.text(packTime)
                                    );
                                } catch (Exception e) {
                                    handleFeedback(context, MessageConstants.COMMAND_RELOAD_PACK_FAILURE);
                                    plugin().logger().warn("Failed to generate resource pack", e);
                                } finally {
                                    RELOAD_PACK_FLAG = false;
                                }
                            }, plugin().scheduler().async());
                        } catch (Exception e) {
                            handleFeedback(context, MessageConstants.COMMAND_RELOAD_ALL_FAILURE);
                            plugin().logger().warn("Failed to generate resource pack", e);
                        }
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
