package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RemoveItemComponentCommand extends BukkitCommandFeature<CommandSender> {
    private final List<@NonNull Suggestion> suggestions;

    public RemoveItemComponentCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
        this.suggestions = VersionHelper.COMPONENT_RELEASE ? RegistryProxy.INSTANCE.keySet(BuiltInRegistriesProxy.DATA_COMPONENT_TYPE).stream()
                .map(Object::toString)
                .map(Suggestion::suggestion)
                .toList() : List.of();
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .required("component", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(RemoveItemComponentCommand.this.suggestions);
                    }
                }))
                .handler(context -> {
                    String component = context.get("component").toString();
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(context.sender());
                    if (serverPlayer == null) return;
                    Item itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                    if (itemInHand.isEmpty()) {
                        handleFeedback(context, MessageConstants.COMMAND_PLAYER_ITEMLESS, Component.text(serverPlayer.name()));
                        return;
                    }
                    itemInHand.removeComponent(component);
                    handleFeedback(context, MessageConstants.COMMAND_ITEM_REMOVE_ITEM_COMPONENT, Component.text(component));
                });
    }

    @Override
    public String getFeatureID() {
        return "remove_item_component";
    }

    @Override
    public boolean isAvailable() {
        return VersionHelper.COMPONENT_RELEASE;
    }
}
