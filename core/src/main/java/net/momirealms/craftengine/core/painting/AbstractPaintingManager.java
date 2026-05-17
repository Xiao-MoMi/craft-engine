package net.momirealms.craftengine.core.painting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractPaintingManager implements PaintingManager {
    protected final CraftEngine plugin;
    protected final ConfigParser paintingParser;
    protected final Map<Key, Painting> paintings = new HashMap<>();

    protected AbstractPaintingManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.paintingParser = new PaintingParser();
        this.registerPaintings(loadLastRegisteredPaintings());
    }

    private Map<Key, Painting> loadLastRegisteredPaintings() {
        Path persistPaintingPath = this.plugin.dataFolderPath()
                .resolve("cache")
                .resolve("paintings.json");
        if (Files.exists(persistPaintingPath) && Files.isRegularFile(persistPaintingPath)) {
            try {
                Map<Key, Painting> paintings = new HashMap<>();
                JsonObject cache = GsonHelper.readJsonFromFile(persistPaintingPath).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : cache.entrySet()) {
                    Key id = Key.of(entry.getKey());
                    if (entry.getValue() instanceof JsonObject json) {
                        paintings.put(id, Painting.fromJson(json));
                    }
                }
                return paintings;
            } catch (IOException e) {
                this.plugin.logger().warn("Failed to load registered paintings", e);
            }
        }
        return Map.of();
    }

    private void saveLastRegisteredPaintings(Map<Key, Painting> paintings) {
        if (paintings.isEmpty()) return;
        Path persistPaintingPath = this.plugin.dataFolderPath()
                .resolve("cache")
                .resolve("paintings.json");
        try {
            Files.createDirectories(persistPaintingPath.getParent());
            JsonObject cache = new JsonObject();
            for (Map.Entry<Key, Painting> entry : paintings.entrySet()) {
                cache.add(entry.getKey().asString(), entry.getValue().toJson());
            }
            GsonHelper.writeJsonFile(cache, persistPaintingPath);
        } catch (IOException e) {
            this.plugin.logger().warn("Failed to save registered paintings", e);
        }
    }

    @Override
    public ConfigParser parser() {
        return this.paintingParser;
    }

    @Override
    public void disable() {
        this.saveLastRegisteredPaintings(this.paintings);
    }

    @Override
    public void unload() {
        this.paintings.clear();
    }

    @Override
    public void runDelayedSyncTasks() {
        if (!VersionHelper.isOrAbove1_21) return;
        this.registerPaintings(this.paintings);
    }

    protected abstract void registerPaintings(Map<Key, Painting> paintings);

    private final class PaintingParser extends IdSectionConfigParser {
        private static final String[] CONFIG_SECTION_NAME = new String[]{"paintings", "painting"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.PAINTING;
        }

        @Override
        public int count() {
            return AbstractPaintingManager.this.paintings.size();
        }

        private static final String[] ASSET_ID = new String[]{"asset_id", "asset-id"};
        private static final String[] SHOW_IN_OP_TAB = new String[]{"show_in_op_tab", "show-in-op-tab"};

        @Override
        protected void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            int width = section.getValue("width", it -> it.getAsInt(1, 16), 1);
            int height = section.getValue("height", it -> it.getAsInt(1, 16), 1);
            Key assetId = section.getIdentifier(ASSET_ID, id);
            Component title = section.getValue("title", ConfigValue::getAsComponent);
            Component author = section.getValue("author", ConfigValue::getAsComponent);
            boolean showInOpTab = section.getBoolean(SHOW_IN_OP_TAB);
            Painting painting = new Painting(width, height, assetId, Optional.ofNullable(title), Optional.ofNullable(author), showInOpTab);
            AbstractPaintingManager.this.paintings.put(id, painting);
        }
    }
}
