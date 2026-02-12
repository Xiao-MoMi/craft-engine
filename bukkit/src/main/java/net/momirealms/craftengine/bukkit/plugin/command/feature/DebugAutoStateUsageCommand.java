package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.block.AutoStateGroup;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.pack.allocator.BlockStateCandidate;
import net.momirealms.craftengine.core.pack.allocator.VisualBlockStateAllocator;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DebugAutoStateUsageCommand extends BukkitCommandFeature<CommandSender> {

    public DebugAutoStateUsageCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("id", StringParser.stringComponent(StringParser.StringMode.GREEDY_FLAG_YIELDING).suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        List<String> suggestions = new ArrayList<>();
                        for (AutoStateGroup stateGroup : AutoStateGroup.values()) {
                            if (!stateGroup.candidates().isEmpty()) {
                                suggestions.addAll(stateGroup.ids());
                            }
                        }
                        return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
                    }
                }))
                .handler(context -> {
                    String group = context.get("id");
                    BukkitBlockManager blockManager = plugin().blockManager();
                    AutoStateGroup stateGroup = AutoStateGroup.byId(group);
                    if (stateGroup == null) return;
                    List<BlockStateCandidate> candidates = stateGroup.candidates();
                    if (candidates.isEmpty()) return;
                    int i = 0;
                    plugin().senderFactory().wrap(context.sender()).sendMessage(Component.text(stateGroup.id() + ": "));
                    VisualBlockStateAllocator allocator = blockManager.blockParser().visualBlockStateAllocator();
                    Map<String, BlockStateWrapper> cachedStates = allocator.cachedBlockStates();
                    Map<BlockStateWrapper, String> reversed = new HashMap<>(cachedStates.size());
                    for (Map.Entry<String, BlockStateWrapper> entry : cachedStates.entrySet()) {
                        reversed.put(entry.getValue(), entry.getKey());
                    }
                    List<Component> batch = new ArrayList<>();
                    for (BlockStateCandidate candidate : candidates) {
                        BlockStateWrapper appearance = candidate.blockState();
                        Key baseBlockId = appearance.ownerId();
                        Component text = Component.text("|");
                        List<Integer> reals = blockManager.appearanceToRealStates(appearance.registryId());
                        if (reals.isEmpty()) {
                            String cached = reversed.get(appearance);
                            if (cached != null) {
                                Component hover = Component.text("[Inactive] " + baseBlockId.value() + ":" + i).color(NamedTextColor.GRAY);
                                hover = hover.append(Component.newline()).append(Component.text(cached).color(NamedTextColor.GRAY));
                                text = text.color(NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(hover));
                            } else {
                                Component hover = Component.text("[Available] " + baseBlockId.value() + ":" + i).color(NamedTextColor.GREEN);
                                hover = hover.append(Component.newline()).append(Component.text(appearance.getAsString()).color(NamedTextColor.GREEN));
                                text = text.color(NamedTextColor.GREEN).hoverEvent(HoverEvent.showText(hover));
                            }
                        } else {
                            boolean forced = allocator.isForcedState(appearance);
                            NamedTextColor namedTextColor = forced ? NamedTextColor.RED : NamedTextColor.YELLOW;
                            Component hover = Component.text((forced ? "[Forced] " : "[Auto] ") + baseBlockId.value() + ":" + i).color(namedTextColor);
                            List<Component> hoverChildren = new ArrayList<>();
                            hoverChildren.add(Component.newline());
                            hoverChildren.add(Component.text(appearance.getAsString()).color(namedTextColor));
                            for (int real : reals) {
                                hoverChildren.add(Component.newline());
                                hoverChildren.add(Component.text(blockManager.getImmutableBlockStateUnsafe(real).toString()).color(NamedTextColor.GRAY));
                            }
                            text = text.color(namedTextColor).hoverEvent(HoverEvent.showText(hover.children(hoverChildren)));
                        }
                        batch.add(text);
                        i++;
                        if (batch.size() == 100) {
                            plugin().senderFactory().wrap(context.sender())
                                    .sendMessage(Component.text("").children(batch));
                            batch.clear();
                        }
                    }
                    if (!batch.isEmpty()) {
                        plugin().senderFactory().wrap(context.sender())
                                .sendMessage(Component.text("").children(batch));
                        batch.clear();
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_auto_state_usage";
    }
}
