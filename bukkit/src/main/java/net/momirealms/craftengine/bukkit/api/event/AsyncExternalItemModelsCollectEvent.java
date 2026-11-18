package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.pack.ExternalItemModel;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class AsyncExternalItemModelsCollectEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final MinecraftVersion targetVersion;
    private final Map<String, List<ExternalItemModel>> contributions = new LinkedHashMap<>();
    private boolean frozen;

    @ApiStatus.Internal
    public AsyncExternalItemModelsCollectEvent(MinecraftVersion targetVersion) {
        super(true);
        this.targetVersion = targetVersion;
    }

    public MinecraftVersion targetVersion() {
        return this.targetVersion;
    }

    public void contribute(Plugin owner, Collection<ExternalItemModel> models) {
        if (this.frozen) {
            throw new IllegalStateException("External item model collection has finished");
        }
        this.contributions.computeIfAbsent(owner.getName(), ignored -> new ArrayList<>()).addAll(models);
    }

    @ApiStatus.Internal
    public Map<String, List<ExternalItemModel>> freeze() {
        this.frozen = true;
        Map<String, List<ExternalItemModel>> snapshot = new LinkedHashMap<>();
        this.contributions.forEach((owner, models) -> snapshot.put(owner, List.copyOf(models)));
        return Map.copyOf(snapshot);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
