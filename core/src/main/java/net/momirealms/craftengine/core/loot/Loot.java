package net.momirealms.craftengine.core.loot;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface Loot {

    List<Item> getRandomItems(LootContext context);

    List<Item> getRandomItems(ContextHolder parameters, World world);

    List<Item> getRandomItems(ContextHolder parameters, World world, @Nullable Player player);

    void getRandomItems(LootContext context, Consumer<Item> lootConsumer);

}
