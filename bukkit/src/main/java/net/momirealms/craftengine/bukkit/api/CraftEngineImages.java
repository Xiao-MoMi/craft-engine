package net.momirealms.craftengine.bukkit.api;

import net.momirealms.craftengine.bukkit.font.BukkitFontManager;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class CraftEngineImages {

    private CraftEngineImages() {}

    /**
     * Returns an unmodifiable map of all currently loaded custom images.
     * The map keys represent unique identifiers, and the values are the corresponding Image instances.
     *
     * <p><strong>Important:</strong> Do not attempt to access this method during the onEnable phase
     * as it will be empty. Instead, listen for the {@code CraftEngineReloadEvent} and use this method
     * after the event is fired to obtain the complete image list.
     *
     * @return a non-null map containing all loaded custom images
     */
    @NotNull
    public static Map<Key, Image> loadedImages() {
        return BukkitFontManager.instance().loadedImages();
    }

    /**
     * Gets a custom image by ID
     *
     * @param id id
     * @return the custom image
     */
    @Nullable
    public static Image byId(@NotNull Key id) {
        return BukkitFontManager.instance().imageById(id).orElse(null);
    }
}
