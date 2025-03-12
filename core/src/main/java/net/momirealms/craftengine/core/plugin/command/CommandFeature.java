package net.momirealms.craftengine.core.plugin.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.momirealms.craftengine.core.plugin.Plugin;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;

public interface CommandFeature<C> {

    Command<C> registerCommand(org.incendo.cloud.CommandManager<C> cloudCommandManager, Command.Builder<C> builder);

    String getFeatureID();

    void registerRelatedFunctions();

    void unregisterRelatedFunctions();

    void handleFeedback(CommandContext<?> context, TranslatableComponent.Builder key, Component... args);

    void handleFeedback(C sender, TranslatableComponent.Builder key, Component... args);

    CraftEngineCommandManager<C> commandManager();

    CommandConfig<C> commandConfig();

    Plugin plugin();
}
