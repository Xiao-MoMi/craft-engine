package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.block.BlockManager;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.pack.allocator.IdAllocator;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.config.Config;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DebugRealStateUsageCommand extends BukkitCommandFeature<CommandSender> {

    public DebugRealStateUsageCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .handler(context -> {
                    BukkitBlockManager blockManager = plugin().blockManager();
                    plugin().senderFactory().wrap(context.sender()).sendMessage(Component.text("Serverside block state usage:"));
                    List<Component> batch = new ArrayList<>(100);
                    IdAllocator idAllocator = blockManager.internalIdAllocator();
                    Map<String, Integer> cachedIds = idAllocator.cachedIdMap();
                    Map<Integer, String> reversedCachedIds = new HashMap<>(cachedIds.size());
                    for (Map.Entry<String, Integer> entry : cachedIds.entrySet()) {
                        reversedCachedIds.put(entry.getValue(), entry.getKey());
                    }
                    for (int i = 0; i < Config.serverSideBlocks(); i++) {
                        ImmutableBlockState state = blockManager.getImmutableBlockStateUnsafe(i + blockManager.vanillaBlockStateCount());
                        if (state.isEmpty()) {
                            String cached = reversedCachedIds.get(i);
                            if (cached == null) {
                                Component hover = Component.text("[Available] " + BlockManager.createCustomBlockKey(i).asString()).color(NamedTextColor.GREEN);
                                batch.add(Component.text("|").color(NamedTextColor.GREEN).hoverEvent(HoverEvent.showText(hover)));
                            } else {
                                Component hover = Component.text(BlockManager.createCustomBlockKey(i).asString()).color(NamedTextColor.GRAY);
                                hover = hover.append(Component.newline()).append(Component.text("[Inactive] " + cached).color(NamedTextColor.GRAY));
                                batch.add(Component.text("|").color(NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(hover)));
                            }
                        } else {
                            boolean forced = idAllocator.isForced(state.toString());
                            if (forced) {
                                Component hover = Component.text("[Forced] " + BlockManager.createCustomBlockKey(i).asString()).color(NamedTextColor.RED);
                                hover = hover.append(Component.newline()).append(Component.text(state.toString()).color(NamedTextColor.GRAY));
                                batch.add(Component.text("|").color(NamedTextColor.RED).hoverEvent(HoverEvent.showText(hover)));
                            } else {
                                Component hover = Component.text("[Auto] " + BlockManager.createCustomBlockKey(i).asString()).color(NamedTextColor.YELLOW);
                                hover = hover.append(Component.newline()).append(Component.text(state.toString()).color(NamedTextColor.GRAY));
                                batch.add(Component.text("|").color(NamedTextColor.YELLOW).hoverEvent(HoverEvent.showText(hover)));
                            }
                        }
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
        return "debug_real_state_usage";
    }
}
