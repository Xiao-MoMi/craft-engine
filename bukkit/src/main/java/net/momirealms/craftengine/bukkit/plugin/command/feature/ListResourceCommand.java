package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.AdventureHelper;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ListResourceCommand extends BukkitCommandFeature<CommandSender> {

    public ListResourceCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .handler(context -> {
                    Collection<Pack> packs = plugin().packManager().loadedPacks();
                    List<Pack> enabled = new ArrayList<>();
                    List<Pack> disabled = new ArrayList<>();
                    for (Pack pack : packs) {
                        if (pack.enabled()) {
                            enabled.add(pack);
                        } else {
                            disabled.add(pack);
                        }
                    }
                    handleFeedback(context, MessageConstants.COMMAND_RESOURCE_LIST, Component.text(enabled.size()), Component.empty().children(getChildComponents(enabled)), Component.text(disabled.size()), Component.empty().children(getChildComponents(disabled)));
                });
    }

    private List<Component> getChildComponents(List<Pack> disabled) {
        List<Component> components = new ArrayList<>();
        for (int i = 0; i < disabled.size(); i++) {
            Pack pack = disabled.get(i);
            components.add(getPackComponent(pack));
            if (i != disabled.size() - 1) {
                components.add(Component.text(", "));
            }
        }
        if (components.isEmpty()) {
            return List.of(Component.text("[]"));
        }
        return components;
    }

    private Component getPackComponent(Pack pack) {
        String description = pack.meta().description();
        String version = pack.meta().version();
        String author = pack.meta().author();
        String text = version == null ? pack.name() : pack.name() + " v" + version;
        Component base = Component.text("[" + text + "]");
        if (author != null || description != null) {
            if (author != null && description != null) {
                base = base.hoverEvent(HoverEvent.showText(Component.empty().children(List.of(Component.text("by: " + author).color(NamedTextColor.YELLOW), Component.newline(), AdventureHelper.miniMessage().deserialize(description)))));
            } else if (author != null) {
                base = base.hoverEvent(HoverEvent.showText(Component.text("by: " + author)));
            } else {
                base = base.hoverEvent(HoverEvent.showText(AdventureHelper.miniMessage().deserialize(description)));
            }
        }
        return base;
    }

    @Override
    public String getFeatureID() {
        return "list_resource";
    }
}
