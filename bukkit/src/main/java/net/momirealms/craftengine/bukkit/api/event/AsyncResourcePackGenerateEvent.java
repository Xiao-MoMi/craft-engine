package net.momirealms.craftengine.bukkit.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class AsyncResourcePackGenerateEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Path generatedPackPath;
    private final Path zipFilePath;

    @ApiStatus.Internal
    public AsyncResourcePackGenerateEvent(@NotNull Path generatedPackPath,
                                          @NotNull Path zipFilePath) {
        super(true);
        this.generatedPackPath = generatedPackPath;
        this.zipFilePath = zipFilePath;
    }

    @NotNull
    public Path resourcePackFolder() {
        return generatedPackPath;
    }

    @NotNull
    public Path zipFilePath() {
        return zipFilePath;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    public HandlerList getHandlers() {
        return getHandlerList();
    }
}
