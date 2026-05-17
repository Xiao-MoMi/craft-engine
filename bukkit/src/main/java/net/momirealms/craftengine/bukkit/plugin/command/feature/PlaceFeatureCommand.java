package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerChunkCacheProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerLevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.levelgen.feature.ConfiguredFeatureProxy;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;
import org.incendo.cloud.bukkit.parser.location.LocationParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;

public final class PlaceFeatureCommand extends BukkitCommandFeature<CommandSender> {
    private static final int[][] CHUNK_OFFSETS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0},  {0, 0},  {1, 0},
            {-1, 1},  {0, 1},  {1, 1}
    };

    public PlaceFeatureCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .flag(FlagKeys.SILENT_FLAG)
                .required("feature", NamespacedKeyParser.namespacedKeyComponent().suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(BukkitWorldManager.instance().cachedConfiguredFeaturesSuggestion());
                    }
                }))
                .optional("location", LocationParser.locationParser())
                .handler(context -> {
                    Key id = KeyUtils.namespacedKeyToKey(context.get("feature"));
                    Object feature = BukkitWorldManager.instance().configuredFeatureHolderById(id);
                    Player sender = context.sender();
                    if (feature == null) {
                        handleFeedback(sender, MessageConstants.COMMAND_PLACE_FEATURE_INVALID, Component.text(id.asString()));
                        return;
                    }
                    BukkitServerPlayer player = BukkitAdaptor.adapt(sender);
                    if (player == null) return;
                    BlockPos pos;
                    if (context.contains("location")) {
                        Location location = context.get("location");
                        pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    } else {
                        pos = new BlockPos(MiscUtils.floor(player.x()), MiscUtils.floor(player.y()), MiscUtils.floor(player.z()));
                    }
                    Object level = player.world().minecraftWorld();
                    Object configuredFeature = HolderProxy.INSTANCE.value(feature);
                    int chunkX = pos.x >> 4;
                    int chunkZ = pos.z >> 4;
                    for (int[] offset : CHUNK_OFFSETS) {
                        int targetX = chunkX + offset[0];
                        int targetZ = chunkZ + offset[1];
                        if (!LevelProxy.INSTANCE.isLoaded(level, BlockPosProxy.INSTANCE.newInstance(targetX << 4, 0, targetZ << 4))) {
                            handleFeedback(sender, MessageConstants.COMMAND_CHUNK_NOT_LOADED);
                            return;
                        }
                    }
                    Object chunkGenerator = ServerChunkCacheProxy.INSTANCE.getGenerator(ServerLevelProxy.INSTANCE.getChunkSource(level));
                    Object randomSource = LevelAccessorProxy.INSTANCE.getRandom(level);
                    if (ConfiguredFeatureProxy.INSTANCE.place(configuredFeature, level, chunkGenerator, randomSource, LocationUtils.toBlockPos(pos))) {
                        handleFeedback(sender, MessageConstants.COMMAND_PLACE_FEATURE_SUCCESS, Component.text(id.asString()), Component.text(pos.x), Component.text(pos.y), Component.text(pos.z));
                    } else {
                        handleFeedback(sender, MessageConstants.COMMAND_PLACE_FEATURE_FAILED);
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "place_feature";
    }
}
