package net.momirealms.craftengine.bukkit.plugin.command.debug;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DebugItemComponentCommand extends BukkitCommandFeature<CommandSender> {
    private final List<@NonNull Suggestion> componentSuggestions;

    public DebugItemComponentCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
        this.componentSuggestions = VersionHelper.COMPONENT_RELEASE
                ? RegistryProxy.INSTANCE.keySet(BuiltInRegistriesProxy.DATA_COMPONENT_TYPE).stream()
                  .map(Object::toString)
                  .map(Suggestion::suggestion)
                  .toList()
                : List.of();
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .required("component", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(DebugItemComponentCommand.this.componentSuggestions);
                    }
                }))
                .required("format", EnumParser.enumComponent(OutputFormat.class))
                .handler(context -> {
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(context.sender());
                    if (serverPlayer == null) return;

                    Item itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                    if (itemInHand.isEmpty()) {
                        handleFeedback(context, MessageConstants.COMMAND_PLAYER_ITEMLESS, Component.text(serverPlayer.name()));
                        return;
                    }

                    NamespacedKey componentKey = context.get("component");
                    Object componentType = RegistryUtils.getRegistryValue(
                            BuiltInRegistriesProxy.DATA_COMPONENT_TYPE,
                            KeyUtils.toIdentifier(KeyUtils.namespacedKeyToKey(componentKey))
                    );
                    if (componentType == null) {
                        sendError(context, "Unknown data component type '" + componentKey + "'");
                        return;
                    }
                    if (itemInHand.getExactComponent(componentType) == null) {
                        sendError(context, "The held item does not have component '" + componentKey + "'");
                        return;
                    }

                    OutputFormat format = context.get("format");
                    String serialized = format.serialize(itemInHand, componentType);
                    if (serialized == null) {
                        sendError(context, "Failed to serialize component '" + componentKey + "' as " + format.name());
                        return;
                    }

                    String value = format.prefix + serialized;
                    Component message = Component.text(componentKey + " (" + format.name() + "): ", NamedTextColor.GRAY)
                            .append(Component.text(value, NamedTextColor.GOLD)
                                    .hoverEvent(HoverEvent.showText(Component.translatable("chat.copy.click", NamedTextColor.WHITE)))
                                    .clickEvent(ClickEvent.copyToClipboard(value)));
                    plugin().senderFactory().wrap(context.sender()).sendMessage(message);
                });
    }

    private void sendError(CommandContext<? extends CommandSender> context, String message) {
        plugin().senderFactory().wrap(context.sender()).sendMessage(Component.text(message, NamedTextColor.RED));
    }

    @Override
    public String getFeatureID() {
        return "debug_item_component";
    }

    @Override
    public boolean isAvailable() {
        return VersionHelper.COMPONENT_RELEASE;
    }

    private enum OutputFormat {
        JSON("(json) ") {
            @Override
            String serialize(Item item, Object componentType) {
                JsonElement json = item.getComponentAsJson(componentType);
                return json == null ? null : GsonHelper.toString(json);
            }
        },
        SNBT("(snbt) ") {
            @Override
            String serialize(Item item, Object componentType) {
                Tag tag = item.getComponentAsSparrowTag(componentType);
                return tag == null ? null : tag.toString();
            }
        };

        private final String prefix;

        OutputFormat(String prefix) {
            this.prefix = prefix;
        }

        abstract String serialize(Item item, Object componentType);
    }
}
