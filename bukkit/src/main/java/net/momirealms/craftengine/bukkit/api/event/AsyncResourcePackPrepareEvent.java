package net.momirealms.craftengine.bukkit.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Runs after storage/default resolution, before requesting URLs. Delivery waits for listeners.
 * The player may still be in configuration state. Modify packIds() to add/remove configured IDs.
 * No preference is persisted by this event. Pack order always follows configuration.
 */
public final class AsyncResourcePackPrepareEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;
    private final List<String> packIds;

    public AsyncResourcePackPrepareEvent(UUID playerId, String playerName, List<String> packIds) {
        super(true);
        this.playerId = playerId;
        this.playerName = playerName;
        this.packIds = packIds;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public UUID playerId() {
        return this.playerId;
    }

    public String playerName() {
        return this.playerName;
    }

    public List<String> packIds() {
        return this.packIds;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
