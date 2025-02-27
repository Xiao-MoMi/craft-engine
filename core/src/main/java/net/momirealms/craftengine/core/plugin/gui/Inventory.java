package net.momirealms.craftengine.core.plugin.gui;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.Nullable;

public interface Inventory {

    void open(Player player, Component title);

    void setItem(int index, @Nullable Item<?> item);
}
