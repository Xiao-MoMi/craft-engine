package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MSoundEvents;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.PlayerUtils;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.sound.SoundSourceProxy;
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
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TotemAnimationCommand extends BukkitCommandFeature<CommandSender> {
    public static final Object FIX_TOTEM_SOUND_PACKET = FastNMS.INSTANCE.constructor$ClientboundSoundPacket(FastNMS.INSTANCE.method$Holder$direct(MSoundEvents.TOTEM_USE), SoundSourceProxy.MUSIC, 0, Integer.MIN_VALUE, 0, 0, 0, 0);

    public TotemAnimationCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .flag(CommandFlag.builder("no-sound"))
                .required("players", MultiplePlayerSelectorParser.multiplePlayerSelectorParser())
                .required("id", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().itemManager().cachedTotemSuggestions());
                    }
                }))
                .optional("sound", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().soundManager().cachedSoundSuggestions());
                    }
                }))
                .optional("volume", FloatParser.floatParser(0f))
                .optional("pitch", FloatParser.floatParser(0f, 2f))
                .optional("min-volume", FloatParser.floatParser(0f))
                .optional("min-pitch", FloatParser.floatParser(0f, 2f))
                .handler(context -> {
                    NamespacedKey namespacedKey = context.get("id");
                    Key key = Key.of(namespacedKey.namespace(), namespacedKey.value());
                    CustomItem<ItemStack> customItem = plugin().itemManager().getCustomItem(key).orElse(null);
                    if (customItem == null || (!VersionHelper.isOrAbove1_21_2() && customItem.material().equals(ItemKeys.TOTEM_OF_UNDYING))) {
                        handleFeedback(context, MessageConstants.COMMAND_TOTEM_NOT_TOTEM, Component.text(key.toString()));
                        return;
                    }
                    Optional<NamespacedKey> soundKey = context.optional("sound");
                    SoundData soundData = null;
                    if (soundKey.isPresent()) {
                        float volume = context.getOrDefault("volume", 1.0f);
                        float pitch = context.getOrDefault("pitch", 1.0f);
                        float minVolume = context.getOrDefault("min-volume", 1.0f);
                        float minPitch = context.getOrDefault("min-pitch", 1.0f);
                        soundData = SoundData.of(KeyUtils.namespacedKeyToKey(soundKey.get()), SoundData.SoundValue.ranged(minVolume, volume), SoundData.SoundValue.ranged(minPitch, pitch));
                    }
                    boolean removeSound = context.flags().hasFlag("no-sound");
                    MultiplePlayerSelector selector = context.get("players");
                    Collection<Player> players = selector.values();
                    for (Player player : players) {
                        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
                        if (serverPlayer == null) continue;
                        Item<ItemStack> item = customItem.buildItem(serverPlayer);
                        if (VersionHelper.isOrAbove1_21_2()) {
                            item.setJavaComponent(DataComponentTypes.DEATH_PROTECTION, Map.of());
                        }
                        PlayerUtils.sendTotemAnimation(serverPlayer, item, soundData, removeSound);
                    }

                    if (players.size() == 1) {
                        handleFeedback(context, MessageConstants.COMMAND_TOTEM_SUCCESS_SINGLE, Component.text(namespacedKey.toString()), Component.text(players.iterator().next().getName()));
                    } else if (players.size() > 1) {
                        handleFeedback(context, MessageConstants.COMMAND_TOTEM_SUCCESS_MULTIPLE, Component.text(namespacedKey.toString()), Component.text(players.size()));
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "totem_animation";
    }
}
