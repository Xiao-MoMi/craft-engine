package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.item.ItemManager;
import net.momirealms.craftengine.core.pack.model.definition.ModernItemModel;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Collects item definitions, models and textures contributed by other plugins for the resource pack
 * that is about to be generated.
 * <p>
 * Content is buffered here rather than written straight into the pack so that everything CraftEngine
 * itself generates takes precedence, and so contributors never get a reference to CraftEngine's own
 * mutable registries.
 * <p>
 * Insertion order is preserved to keep pack generation reproducible. This type is not thread safe;
 * it is populated by a single synchronous dispatch and consumed immediately afterwards.
 */
public final class PackInjection {
    private final ItemManager itemManager;
    private final MinecraftVersion packMinVersion;
    private final MinecraftVersion packMaxVersion;
    private final Map<Key, ModernItemModel> itemDefinitions = new LinkedHashMap<>();
    private final Set<Key> obfuscatableItemDefinitions = new LinkedHashSet<>();
    private final Map<Key, ModelGeneration> models = new LinkedHashMap<>();
    private final Map<Key, byte[]> textures = new LinkedHashMap<>();

    @ApiStatus.Internal
    public PackInjection(@NotNull ItemManager itemManager, @NotNull MinecraftVersion packMinVersion, @NotNull MinecraftVersion packMaxVersion) {
        this.itemManager = itemManager;
        this.packMinVersion = packMinVersion;
        this.packMaxVersion = packMaxVersion;
    }

    /**
     * Registers an item definition, written to {@code assets/<namespace>/items/<path>.json}.
     * <p>
     * Equivalent to {@link #registerItemDefinition(Key, ModernItemModel, boolean)} with obfuscation
     * disabled, which is the safe default: the key stays exactly as given, so a plugin that sets the
     * {@code minecraft:item_model} component itself can rely on it.
     *
     * @param itemModel the item model key
     * @param model     the definition to write
     */
    public void registerItemDefinition(@NotNull Key itemModel, @NotNull ModernItemModel model) {
        this.registerItemDefinition(itemModel, model, false);
    }

    /**
     * Registers an item definition, written to {@code assets/<namespace>/items/<path>.json}.
     * <p>
     * Pass {@code allowObfuscation} only if the caller resolves the key through
     * {@code CraftEngineItems#clientItemModel(Key)} every time it sends the item. Item model
     * obfuscation renames the definition file and remaps the key, and the mapping is not known until
     * after generation finishes, so a cached key will be stale.
     *
     * @param itemModel        the item model key
     * @param model            the definition to write
     * @param allowObfuscation whether this key may be renamed by item model obfuscation
     */
    public void registerItemDefinition(@NotNull Key itemModel, @NotNull ModernItemModel model, boolean allowObfuscation) {
        Objects.requireNonNull(itemModel, "itemModel");
        this.itemDefinitions.put(itemModel, Objects.requireNonNull(model, "model"));
        if (allowObfuscation) {
            this.obfuscatableItemDefinitions.add(itemModel);
        } else {
            this.obfuscatableItemDefinitions.remove(itemModel);
        }
    }

    /**
     * Registers a model, written to {@code assets/<namespace>/models/<path>.json}. Any raw textures
     * carried by the {@link ModelGeneration} are written too.
     *
     * @param path  the model key
     * @param model the model to write
     */
    public void registerModel(@NotNull Key path, @NotNull ModelGeneration model) {
        this.models.put(Objects.requireNonNull(path, "path"), Objects.requireNonNull(model, "model"));
    }

    /**
     * Registers a texture, written to {@code assets/<namespace>/textures/<path>.png}.
     * <p>
     * Note that the path decides atlas coverage. A key under {@code item/} or {@code block/} is picked
     * up by the vanilla atlas directory sources in every namespace; anywhere else needs its own atlas
     * entry, or the sprite is never stitched.
     *
     * @param path the texture key, without the {@code .png} extension
     * @param png  the PNG bytes
     */
    public void registerTexture(@NotNull Key path, byte @NotNull [] png) {
        this.textures.put(Objects.requireNonNull(path, "path"), Objects.requireNonNull(png, "png"));
    }

    /**
     * @return the item manager, for resolving the models that existing items render with
     */
    @NotNull
    public ItemManager itemManager() {
        return this.itemManager;
    }

    /**
     * @return the lowest pack format version this pack targets
     */
    @NotNull
    public MinecraftVersion packMinVersion() {
        return this.packMinVersion;
    }

    /**
     * @return the highest pack format version this pack targets
     */
    @NotNull
    public MinecraftVersion packMaxVersion() {
        return this.packMaxVersion;
    }

    @ApiStatus.Internal
    public Map<Key, ModernItemModel> itemDefinitions() {
        return Collections.unmodifiableMap(this.itemDefinitions);
    }

    @ApiStatus.Internal
    public boolean canObfuscate(@NotNull Key itemModel) {
        return this.obfuscatableItemDefinitions.contains(itemModel);
    }

    @ApiStatus.Internal
    public Map<Key, ModelGeneration> models() {
        return Collections.unmodifiableMap(this.models);
    }

    @ApiStatus.Internal
    public Map<Key, byte[]> textures() {
        return Collections.unmodifiableMap(this.textures);
    }

    @ApiStatus.Internal
    public boolean isEmpty() {
        return this.itemDefinitions.isEmpty() && this.models.isEmpty() && this.textures.isEmpty();
    }
}
