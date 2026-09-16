package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.ResourceOperationCoordinator;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Timestamp;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import java.util.concurrent.CompletableFuture;

public final class PackWorkflowCommand extends BukkitCommandFeature<CommandSender> {
    public PackWorkflowCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder.required("name", StringParser.stringParser(), (context, input) -> CompletableFuture.completedFuture(plugin().packManager().workflowNames().stream().map(Suggestion::suggestion).toList()))
                .handler(context -> {
                    if (plugin().resourceOperations().isBusy()) {
                        handleFeedback(context, MessageConstants.COMMAND_RESOURCE_BUSY);
                        return;
                    }
                    String name = context.get("name");
                    if (!plugin().packManager().workflowNames().contains(name)) {
                        handleFeedback(context, MessageConstants.COMMAND_WORKFLOW_UNKNOWN, Component.text(name));
                        return;
                    }
                    handleFeedback(context, MessageConstants.COMMAND_WORKFLOW_STARTED, Component.text(name));
                    plugin().scheduler().executeAsync(() -> {
                        Timestamp timestamp = new Timestamp();
                        try {
                            plugin().packManager().runWorkflow(name);
                            handleFeedback(context, MessageConstants.COMMAND_WORKFLOW_SUCCESS, Component.text(name), Component.text(timestamp.deltaMillis()));
                        } catch (ResourceOperationCoordinator.BusyException e) {
                            handleFeedback(context, MessageConstants.COMMAND_RESOURCE_BUSY);
                        } catch (Exception e) {
                            plugin().logger().warn("Resource pack workflow failed: " + name, e);
                            handleFeedback(context, MessageConstants.COMMAND_WORKFLOW_FAILURE, Component.text(name));
                        }
                    });
                });
    }

    @Override
    public String getFeatureID() {
        return "pack_workflow";
    }
}
