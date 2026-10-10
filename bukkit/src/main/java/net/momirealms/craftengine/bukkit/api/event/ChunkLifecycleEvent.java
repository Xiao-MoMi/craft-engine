package net.momirealms.craftengine.bukkit.api.event;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class ChunkLifecycleEvent extends Event {
    public static final HandlerList HANDLERS = new HandlerList();
    private final World world;
    private final int chunkX;
    private final int chunkZ;
    private final Stage stage;

    @ApiStatus.Internal
    public ChunkLifecycleEvent(World world, int chunkX, int chunkZ, Stage stage) {
        super(!Bukkit.isPrimaryThread());
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.stage = stage;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    public World world() {
        return this.world;
    }

    public int chunkX() {
        return this.chunkX;
    }

    public int chunkZ() {
        return this.chunkZ;
    }

    @NotNull
    public Stage stage() {
        return this.stage;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public enum Stage {
        LOAD_START,
        DATA_READ,
        HOLDER_UNLOAD
    }
}
