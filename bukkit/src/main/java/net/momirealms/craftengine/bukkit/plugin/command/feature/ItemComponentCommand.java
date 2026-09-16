package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.TagParser;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ItemComponentCommand extends BukkitCommandFeature<CommandSender> {
    private final Action action;
    private final List<@NonNull Suggestion> componentSuggestions;

    public ItemComponentCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin, Action action) {
        super(commandManager, plugin);
        this.action = action;
        this.componentSuggestions = VersionHelper.COMPONENT_RELEASE
                ? RegistryProxy.INSTANCE.keySet(BuiltInRegistriesProxy.DATA_COMPONENT_TYPE).stream()
                  .map(Object::toString)
                  .map(Suggestion::suggestion)
                  .toList()
                : List.of();
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        Command.Builder<Player> command = builder
                .senderType(Player.class)
                .required("component", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(ItemComponentCommand.this.componentSuggestions);
                    }
                }));
        if (this.action == Action.ADD) {
            command = command.required("snbt", StringParser.greedyStringParser());
        }
        return command.handler(context -> {
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(context.sender());
            if (serverPlayer == null) return;
            Item itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
            if (itemInHand.isEmpty()) {
                handleFeedback(context, MessageConstants.COMMAND_PLAYER_ITEMLESS, Component.text(serverPlayer.name()));
                return;
            }

            String component = context.get("component").toString();
            Object componentType = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.DATA_COMPONENT_TYPE, KeyUtils.toIdentifier(component));
            if (componentType == null) {
                handleFeedback(context, MessageConstants.COMMAND_ITEM_COMPONENT_UNKNOWN, Component.text(component));
                return;
            }

            switch (this.action) {
                case ADD -> {
                    try {
                        itemInHand.setSparrowTagComponent(componentType, TagParser.parseTagFully(context.get("snbt")));
                    } catch (Exception e) {
                        Throwable cause = e;
                        while (cause.getCause() != null) {
                            cause = cause.getCause();
                        }
                        handleFeedback(context, MessageConstants.COMMAND_ITEM_COMPONENT_INVALID_VALUE,
                                Component.text(component), Component.text(String.valueOf(cause.getMessage())));
                        return;
                    }
                }
                case REMOVE -> itemInHand.removeComponent(componentType);
                case RESET -> itemInHand.resetComponent(componentType);
            }
            handleFeedback(context, this.action.feedback, Component.text(component));
        });
    }

    @Override
    public String getFeatureID() {
        return this.action.featureId;
    }

    @Override
    public boolean isAvailable() {
        return VersionHelper.COMPONENT_RELEASE;
    }

    public enum Action {
        ADD("item_component_add", MessageConstants.COMMAND_ITEM_COMPONENT_ADD),
        REMOVE("item_component_remove", MessageConstants.COMMAND_ITEM_COMPONENT_REMOVE),
        RESET("item_component_reset", MessageConstants.COMMAND_ITEM_COMPONENT_RESET);

        private final String featureId;
        private final TranslatableComponent.Builder feedback;

        Action(String featureId, TranslatableComponent.Builder feedback) {
            this.featureId = featureId;
            this.feedback = feedback;
        }
    }
}
