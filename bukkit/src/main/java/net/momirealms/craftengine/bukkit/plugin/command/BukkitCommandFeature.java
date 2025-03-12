package net.momirealms.craftengine.bukkit.plugin.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.AbstractCommandFeature;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.util.Pair;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.incendo.cloud.bukkit.data.Selector;

import java.util.Collection;

public abstract class BukkitCommandFeature<C extends CommandSender> extends AbstractCommandFeature<C> {

    public BukkitCommandFeature(CraftEngineCommandManager<C> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    public Pair<TranslatableComponent.Builder, Component> resolveSelector(Selector<? extends Entity> selector, TranslatableComponent.Builder single, TranslatableComponent.Builder multiple) {
        Collection<? extends Entity> entities = selector.values();
        if (entities.size() == 1) {
            return Pair.of(single, Component.text(entities.iterator().next().getName()));
        } else {
            return Pair.of(multiple, Component.text(entities.size()));
        }
    }

    public Pair<TranslatableComponent.Builder, Component> resolveSelector(Collection<? extends Entity> selector, TranslatableComponent.Builder single, TranslatableComponent.Builder multiple) {
        if (selector.size() == 1) {
            return Pair.of(single, Component.text(selector.iterator().next().getName()));
        } else {
            return Pair.of(multiple, Component.text(selector.size()));
        }
    }

    @Override
    public BukkitCraftEngine plugin() {
        return (BukkitCraftEngine) super.plugin();
    }
}
