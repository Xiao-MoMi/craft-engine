package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.FlagKeys;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;

public final class DebugGetBlockInternalIdCommand extends BukkitCommandFeature<CommandSender> {

    public DebugGetBlockInternalIdCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .flag(FlagKeys.SILENT_FLAG)
                .required("id", StringParser.stringComponent(StringParser.StringMode.GREEDY_FLAG_YIELDING).suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().blockManager().cachedSuggestions());
                    }
                }))
                .handler(context -> {
                    String data = context.get("id");
                    ImmutableBlockState state = BlockStateParser.deserialize(data);
                    if (state == null) {
                        plugin().senderFactory().wrap(context.sender()).sendMessage(
                                DebugCommandOutput.error("Could not parse block state '" + data + "'"));
                        return;
                    }
                    String id = BlockStateUtils.getBlockOwnerIdFromState(state.customBlockState().minecraftState()).toString();
                    Sender sender = plugin().senderFactory().wrap(context.sender());
                    sender.sendMessage(DebugCommandOutput.title("Block Internal ID"));
                    sender.sendMessage(DebugCommandOutput.value("CraftEngine state", state));
                    sender.sendMessage(DebugCommandOutput.value("Internal ID", id));
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_get_block_internal_id";
    }
}
