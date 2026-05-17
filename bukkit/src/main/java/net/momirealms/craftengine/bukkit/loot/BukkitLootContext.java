package net.momirealms.craftengine.bukkit.loot;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BukkitLootContext extends LootContext {

    public BukkitLootContext(@NotNull World world, @Nullable Player player, float luck, @NotNull ContextHolder contexts) {
        super(world, player, luck, contexts);
    }

    protected abstract Object getMinecraftLootParamsBuilder();

}
