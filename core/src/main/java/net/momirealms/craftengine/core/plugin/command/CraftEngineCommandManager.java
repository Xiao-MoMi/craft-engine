package net.momirealms.craftengine.core.plugin.command;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.util.Index;
import net.momirealms.craftengine.core.util.TriConsumer;
import org.incendo.cloud.Command;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface CraftEngineCommandManager<C> {
    String commandsFile = "commands.yml";

    void unregisterFeatures();

    void registerFeature(CommandFeature<C> feature, CommandConfig<C> config);

    void registerDefaultFeatures();

    Index<String, CommandFeature<C>> features();

    void setFeedbackConsumer(@NotNull TriConsumer<C, String, Component> feedbackConsumer);

    TriConsumer<C, String, Component> defaultFeedbackConsumer();

    CommandConfig<C> getCommandConfig(YamlDocument document, String featureID);

    Collection<Command.Builder<C>> buildCommandBuilders(CommandConfig<C> config);

    org.incendo.cloud.CommandManager<C> getCommandManager();

    void handleCommandFeedback(C sender, TranslatableComponent.Builder key, Component... args);

    void handleCommandFeedback(C sender, String node, Component component);
}
