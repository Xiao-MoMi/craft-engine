package net.momirealms.craftengine.core.loot;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class LootContext extends PlayerOptionalContext {
    private final World world;
    private final float luck;

    public LootContext(@NotNull World world, @Nullable Player player, float luck, @NotNull ContextHolder contexts) {
        super(player, contexts);
        this.world = world;
        this.luck = luck;
    }

    public float luck() {
        return this.luck;
    }

    public World world() {
        return this.world;
    }
}
