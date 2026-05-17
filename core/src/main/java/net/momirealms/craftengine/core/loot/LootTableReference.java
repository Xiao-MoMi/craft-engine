package net.momirealms.craftengine.core.loot;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class LootTableReference implements Loot {
    public final LazyReference<Loot> delegate;

    public LootTableReference(LazyReference<Loot> delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Item> getRandomItems(LootContext context) {
        return this.delegate.get().getRandomItems(context);
    }

    @Override
    public List<Item> getRandomItems(ContextHolder parameters, World world) {
        return this.delegate.get().getRandomItems(parameters, world);
    }

    @Override
    public List<Item> getRandomItems(ContextHolder parameters, World world, @Nullable Player player) {
        return this.delegate.get().getRandomItems(parameters, world, player);
    }

    @Override
    public void getRandomItems(LootContext context, Consumer<Item> lootConsumer) {
        this.delegate.get().getRandomItems(context, lootConsumer);
    }
}
