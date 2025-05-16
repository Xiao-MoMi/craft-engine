package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.bukkit.parser.location.LocationParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DebugSpawnFurnitureCommand extends BukkitCommandFeature<CommandSender> {

    public DebugSpawnFurnitureCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("location", LocationParser.locationParser())
                .required("id", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().furnitureManager().cachedSuggestions());
                    }
                }))
                .optional("anchor-type", EnumParser.enumParser(AnchorType.class))
                .flag(FlagKeys.SILENT_FLAG)
                .handler(context -> {
                    NamespacedKey namespacedKey = context.get("id");
                    Key id = KeyUtils.namespacedKey2Key(namespacedKey);
                    BukkitFurnitureManager furnitureManager = BukkitFurnitureManager.instance();
                    Optional<CustomFurniture> optionalCustomFurniture = furnitureManager.furnitureById(id);
                    if (optionalCustomFurniture.isEmpty()) {
                        return;
                    }
                    Location location = context.get("location");
                    CustomFurniture customFurniture = optionalCustomFurniture.get();
                    AnchorType anchorType = (AnchorType) context.optional("anchor-type").orElse(customFurniture.getAnyPlacement());
                    boolean playSound = context.flags().hasFlag("silent");
                    CraftEngineFurniture.place(location, customFurniture, anchorType, playSound);
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_spawn_furniture";
    }
}
