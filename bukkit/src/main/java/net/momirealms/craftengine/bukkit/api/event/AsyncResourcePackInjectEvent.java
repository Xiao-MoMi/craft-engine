package net.momirealms.craftengine.bukkit.api.event;

import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.pack.PackInjection;
import net.momirealms.craftengine.core.pack.model.definition.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * This event is triggered at the start of resource pack generation, after CraftEngine has finished
 * parsing its own configuration but before any pack content has been written.
 * <p>
 * Use it to contribute item definitions, models and textures to the pack CraftEngine is about to
 * build. Content registered here is written after everything CraftEngine generates itself, so a
 * collision with a CraftEngine-owned file is skipped with a warning rather than overwriting it.
 * </p>
 * <p>
 * Important: you must register your content every time this event is called. Nothing is remembered
 * between pack generations.
 * </p>
 *
 * @see AsyncResourcePackCacheEvent for merging a complete external resource pack instead
 */
public final class AsyncResourcePackInjectEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final PackInjection injection;

    @ApiStatus.Internal
    public AsyncResourcePackInjectEvent(@NotNull PackInjection injection) {
        super(true);
        this.injection = injection;
    }

    @NotNull
    public PackInjection injection() {
        return this.injection;
    }

    /**
     * Registers an item definition, written to {@code assets/<namespace>/items/<path>.json}.
     * The key is left exactly as given.
     *
     * @param itemModel the item model key
     * @param model     the definition to write
     */
    public void registerItemDefinition(@NotNull Key itemModel, @NotNull ModernItemModel model) {
        this.injection.registerItemDefinition(itemModel, model);
    }

    /**
     * Registers an item definition, written to {@code assets/<namespace>/items/<path>.json}.
     *
     * @param itemModel        the item model key
     * @param model            the definition to write
     * @param allowObfuscation whether this key may be renamed by item model obfuscation. Only pass
     *                         {@code true} if you resolve the key through
     *                         {@code CraftEngineItems#clientItemModel(Key)} when sending the item.
     */
    public void registerItemDefinition(@NotNull Key itemModel, @NotNull ModernItemModel model, boolean allowObfuscation) {
        this.injection.registerItemDefinition(itemModel, model, allowObfuscation);
    }

    /**
     * Registers a model, written to {@code assets/<namespace>/models/<path>.json}.
     *
     * @param path  the model key
     * @param model the model to write
     */
    public void registerModel(@NotNull Key path, @NotNull ModelGeneration model) {
        this.injection.registerModel(path, model);
    }

    /**
     * Registers a texture, written to {@code assets/<namespace>/textures/<path>.png}.
     * <p>
     * The path decides atlas coverage: a key under {@code item/} or {@code block/} is picked up by the
     * vanilla atlas directory sources in every namespace, anywhere else needs its own atlas entry.
     *
     * @param path the texture key, without the {@code .png} extension
     * @param png  the PNG bytes
     */
    public void registerTexture(@NotNull Key path, byte @NotNull[] png) {
        this.injection.registerTexture(path, png);
    }

    /**
     * @return the item manager, for resolving the models that existing items render with
     */
    @NotNull
    public ItemManager itemManager() {
        return this.injection.itemManager();
    }

    /**
     * @return the lowest pack format version this pack targets
     */
    @NotNull
    public MinecraftVersion packMinVersion() {
        return this.injection.packMinVersion();
    }

    /**
     * @return the highest pack format version this pack targets
     */
    @NotNull
    public MinecraftVersion packMaxVersion() {
        return this.injection.packMaxVersion();
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
