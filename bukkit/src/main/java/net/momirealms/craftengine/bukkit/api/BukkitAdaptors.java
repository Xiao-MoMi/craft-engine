package net.momirealms.craftengine.bukkit.api;

import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.world.BukkitExistingBlock;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BukkitAdaptors {

    private BukkitAdaptors() {}

    /**
     * Adapts a Bukkit Player to a CraftEngine BukkitServerPlayer.
     * This provides access to CraftEngine-specific player functionality and data.
     *
     * @param player the Bukkit Player to adapt, must not be null
     * @return a non-null BukkitServerPlayer instance wrapping the provided player
     */
    @NotNull
    public static BukkitServerPlayer adapt(@NotNull final Player player) {
        return BukkitCraftEngine.instance().adapt(player);
    }

    /**
     * Adapts a Bukkit World to a CraftEngine BukkitWorld.
     * This enables CraftEngine world operations on Bukkit world instances.
     *
     * @param world the Bukkit World to adapt, must not be null
     * @return a non-null BukkitWorld instance wrapping the provided world
     */
    @NotNull
    public static BukkitWorld adapt(@NotNull final World world) {
        return new BukkitWorld(world);
    }

    /**
     * Adapts a Bukkit Entity to a CraftEngine BukkitEntity.
     * This provides CraftEngine entity functionality for Bukkit entities.
     *
     * @param entity the Bukkit Entity to adapt, must not be null
     * @return a non-null BukkitEntity instance wrapping the provided entity
     */
    @NotNull
    public static BukkitEntity adapt(@NotNull final Entity entity) {
        return new BukkitEntity(entity);
    }

    /**
     * Adapts a Bukkit Block to a CraftEngine BukkitExistingBlock.
     * This enables CraftEngine block operations on Bukkit block instances.
     *
     * @param block the Bukkit Block to adapt, must not be null
     * @return a non-null BukkitExistingBlock instance wrapping the provided block
     */
    @NotNull
    public static BukkitExistingBlock adapt(@NotNull final Block block) {
        return new BukkitExistingBlock(block);
    }
}
