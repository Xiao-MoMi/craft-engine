package net.momirealms.craftengine.bukkit.plugin.command.feature;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DisableResourceCommand extends BukkitCommandFeature<CommandSender> {

    public DisableResourceCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(manager.flagBuilder("silent").withAliases("s"))
                .required("pack", StringParser.stringComponent(StringParser.StringMode.GREEDY).suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().packManager().loadedPacks().stream().filter(Pack::enabled).map(pack -> Suggestion.suggestion(pack.folder().toString().replace("plugins/CraftEngine/resources/", ""))).toList());
                    }
                }))
                .handler(context -> {
                    String packFolder = context.get("pack");
                    Path path = plugin().dataFolderPath().resolve("resources").resolve(packFolder);
                    if (!Files.exists(path)) {
                        handleFeedback(context, MessageConstants.COMMAND_RESOURCE_DISABLE_FAILURE, Component.text(packFolder));
                        return;
                    }
                    Path packMetaPath = path.resolve("pack.yml");
                    if (!Files.exists(packMetaPath)) {
                        try {
                            Files.createFile(packMetaPath);
                        } catch (IOException e) {
                            plugin().logger().warn("Could not create pack.yml file: " + packMetaPath);
                            return;
                        }
                    }
                    YamlDocument document = plugin().config().loadYamlData(packMetaPath);
                    document.set("enable", false);
                    try {
                        document.save(packMetaPath.toFile());
                    } catch (IOException e) {
                        plugin().logger().warn("Could not save pack.yml file: " + packMetaPath);
                        return;
                    }
                    handleFeedback(context, MessageConstants.COMMAND_RESOURCE_DISABLE_SUCCESS, Component.text(packFolder));
                });
    }

    @Override
    public String getFeatureID() {
        return "disable_resource";
    }
}
