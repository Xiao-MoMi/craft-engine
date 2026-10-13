package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.AbstractPackManager;
import net.momirealms.craftengine.core.pack.ResourcePackView;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.Overlays;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JavaResourcePack {
    private final ResourcePackView resources;
    private final MinecraftVersion version;

    public JavaResourcePack(Path root, Overlays overlays, MinecraftVersion version) {
        this.version = version;
        List<Path> roots = new ArrayList<>();
        roots.add(root);
        if (version.isAtOrAbove(MinecraftVersion.V1_20_2)) {
            for (Overlay overlay : overlays.overlays()) {
                if (overlay.test(version)) {
                    roots.add(root.resolve(overlay.directory()));
                }
            }
        }
        this.resources = new ResourcePackView(roots.toArray(Path[]::new));
    }

    public static JavaResourcePack open(Path root, MinecraftVersion version) throws IOException {
        JsonObject metadata = GsonHelper.readJsonObjectFromFile(root.resolve("pack.mcmeta"));
        if (metadata == null) {
            throw new IOException("Missing pack.mcmeta in " + root);
        }
        return new JavaResourcePack(root, new Overlays(metadata), version);
    }

    public MinecraftVersion version() {
        return this.version;
    }

    @Nullable
    public Path findResource(String path) {
        Path resource = this.resources.getExistingReversed(path);
        return resource != null && Files.isRegularFile(resource) ? resource : null;
    }

    public JsonObject readModel(Key model) throws IOException {
        Path resource = this.findResource("assets/" + model.namespace() + "/models/" + model.value() + ".json");
        if (resource == null) {
            JsonObject internal = this.internalModel(model);
            if (internal != null) {
                return internal.deepCopy();
            }
            throw new IOException("Missing Java model " + model + " for " + this.version.version());
        }
        JsonObject json = GsonHelper.readJsonObjectFromFile(resource);
        if (json == null) {
            throw new IOException("Empty Java model " + model + " at " + resource);
        }
        return json;
    }

    @Nullable
    private JsonObject internalModel(Key model) {
        if (!model.isMinecraftNamespace()) {
            return null;
        }
        if (model.value().startsWith("block/")) {
            return AbstractPackManager.PRESET_MODELS_BLOCK.get(Key.minecraft(model.value().substring("block/".length())));
        }
        if (model.value().startsWith("item/")) {
            Key item = Key.minecraft(model.value().substring("item/".length()));
            return this.version.isAtOrAbove(MinecraftVersion.V1_21_4)
                    ? AbstractPackManager.PRESET_MODERN_MODELS_ITEM.get(item)
                    : AbstractPackManager.PRESET_LEGACY_MODELS_ITEM.get(item);
        }
        return null;
    }
}
