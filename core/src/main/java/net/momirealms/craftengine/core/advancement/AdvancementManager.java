package net.momirealms.craftengine.core.advancement;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.Manageable;

public interface AdvancementManager extends Manageable {

    void sendToast(Player player, Item<?> icon, Component message, AdvancementType type);
}
