package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.item.ComponentTypes;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.PlayerUtils;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.data.MultiplePlayerSelector;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.bukkit.parser.selector.MultiplePlayerSelectorParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TotemAnimationCommand extends BukkitCommandFeature<CommandSender> {

    public TotemAnimationCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .required("players", MultiplePlayerSelectorParser.multiplePlayerSelectorParser())
                .required("id", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().itemManager().cachedTotemSuggestions());
                    }
                }))
                .flag(CommandFlag.builder("sound_event").withComponent(NamespacedKeyParser.namespacedKeyParser()).build())
                .flag(CommandFlag.builder("sound_location").withComponent(NamespacedKeyParser.namespacedKeyParser()).build())
                .handler(context -> {
                    NamespacedKey namespacedKey = context.get("id");
                    Key key = Key.of(namespacedKey.namespace(), namespacedKey.value());
                    CustomItem<ItemStack> customItem = plugin().itemManager().getCustomItem(key).orElse(null);
                    if (customItem == null || (!VersionHelper.isOrAbove1_21_2() && customItem.material().equals(ItemKeys.TOTEM_OF_UNDYING))) {
                        handleFeedback(context, MessageConstants.COMMAND_TOTEM_NOT_TOTEM, Component.text(key.toString()));
                        return;
                    }
                    Item<ItemStack> item = customItem.buildItem(ItemBuildContext.EMPTY);
                    if (VersionHelper.isOrAbove1_21_2()) {
                        if (context.flags().contains("sound_location")) {
                            String soundResourceLocation = context.flags().getValue("sound_location").get().toString();
                            if (soundResourceLocation != null) {
                                item.setComponent(ComponentTypes.DEATH_PROTECTION, Map.of("death_effects", List.of(Map.of("type", "play_sound", "sound", Map.of(
                                        "sound_id", soundResourceLocation
                                )))));
                            }
                        } else if (context.flags().contains("sound_event")) {
                            String soundEvent = context.flags().getValue("sound_event").get().toString();
                            if (soundEvent != null) {
                                item.setComponent(ComponentTypes.DEATH_PROTECTION, Map.of("death_effects", List.of(Map.of("type", "play_sound", "sound", soundEvent))));
                            }
                        } else {
                            item.setComponent(ComponentTypes.DEATH_PROTECTION, Map.of());
                        }
                    }
                    ItemStack totemItem = item.getItem();
                    MultiplePlayerSelector selector = context.get("players");
                    for (Player player : selector.values()) {
                        PlayerUtils.sendTotemAnimation(player, totemItem);
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "totem_animation";
    }
}
