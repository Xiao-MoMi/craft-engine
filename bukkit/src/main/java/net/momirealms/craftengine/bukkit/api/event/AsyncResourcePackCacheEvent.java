package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.pack.PackCacheData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This event is triggered when a user executes the "/ce reload pack" command.
 * <p>
 * The event initiates a process that caches all resource content into a virtual file system
 * to ensure optimal build performance. To add your resource pack through this event,
 * you must use the {@link #registerExternalResourcePack(Path)} method every time this event is called.
 * </p>
 * <p>
 * Important: The caching system will not update your resource pack if its file size or
 * last modification time remains unchanged between reloads. Ensure these attributes change
 * if you need the cache to recognize updates.
 * </p>
 */
public final class AsyncResourcePackCacheEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final PackCacheData cacheData;

    @ApiStatus.Internal
    public AsyncResourcePackCacheEvent(@NotNull PackCacheData cacheData) {
        super(true);
        this.cacheData = cacheData;
    }

    @NotNull
    public PackCacheData cacheData() {
        return this.cacheData;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    /**
     * Adds an external resource pack to the cache.
     * <p>
     * This method accepts either a .zip file or a directory path representing a resource pack.
     * The resource pack will be added to the appropriate cache collection based on its type.
     * </p>
     *
     * @param path the file system path to the resource pack. Must be either a .zip file or a directory.
     * @throws IllegalArgumentException if the provided path is neither a .zip file nor a directory.
     */
    public void registerExternalResourcePack(@NotNull final Path path) {
        if (Files.isRegularFile(path) && path.getFileName().endsWith(".zip")) {
            this.cacheData.externalZips().add(path);
        } else if (Files.isDirectory(path)) {
            this.cacheData.externalFolders().add(path);
        } else {
            throw new IllegalArgumentException("Illegal resource pack path: " + path);
        }
    }
}
