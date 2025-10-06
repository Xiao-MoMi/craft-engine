package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DebugAppearanceStateUsageCommand extends BukkitCommandFeature<CommandSender> {

    public DebugAppearanceStateUsageCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("id", StringParser.stringComponent(StringParser.StringMode.GREEDY_FLAG_YIELDING).suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        return CompletableFuture.completedFuture(plugin().blockManager().blockStateArranger().keySet().stream().map(it -> Suggestion.suggestion(it.toString())).toList());
                    }
                }))
                .handler(context -> {
                    String data = context.get("id");
                    BukkitBlockManager blockManager = plugin().blockManager();
                    Key baseBlockId = Key.of(data);
                    List<BlockStateWrapper> appearances = blockManager.blockStateArranger().get(baseBlockId);
                    if (appearances == null) return;
                    int i = 0;
                    Component block = Component.text(baseBlockId + ": ");
                    plugin().senderFactory().wrap(context.sender()).sendMessage(block);
                    VisualBlockStateAllocator allocator = blockManager.blockParser().visualBlockStateAllocator();
                    List<Component> batch = new ArrayList<>();
                    for (BlockStateWrapper appearance : appearances) {
                        Component text = Component.text("|");
                        List<Integer> reals = blockManager.appearanceToRealStates(appearance.registryId());
                        if (reals.isEmpty()) {
                            Component hover = Component.text(baseBlockId.value() + ":" + i).color(NamedTextColor.GREEN);
                            hover = hover.append(Component.newline()).append(Component.text(appearance.getAsString()).color(NamedTextColor.GREEN));
                            text = text.color(NamedTextColor.GREEN).hoverEvent(HoverEvent.showText(hover));
                        } else {
                            boolean isFixed = allocator.isForcedState(appearance);
                            NamedTextColor namedTextColor = isFixed ? NamedTextColor.RED : NamedTextColor.YELLOW;
                            Component hover = Component.text(baseBlockId.value() + ":" + i).color(namedTextColor);
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
        return "debug_appearance_state_usage";
    }
}
