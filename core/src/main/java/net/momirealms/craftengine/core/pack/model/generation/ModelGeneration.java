package net.momirealms.craftengine.core.pack.model.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayMeta;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.EnumUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ModelGeneration implements Supplier<JsonObject> {
    private static final Map<String, BiConsumer<Builder, Object>> BUILDER_FUNCTIONS = new HashMap<>();
    static {
        BUILDER_FUNCTIONS.put("textures", (b, data) -> {
            Map<String, Object> texturesMap = MiscUtils.castToMap(data, false);
            Map<String, String> texturesOverride = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : texturesMap.entrySet()) {
                if (entry.getValue() instanceof String p) {
                    texturesOverride.put(entry.getKey(), p);
                }
            }
            b.texturesOverride(texturesOverride);
        });
        BUILDER_FUNCTIONS.put("display", (b, data) -> {
            Map<String, Object> displayMap = MiscUtils.castToMap(data, false);
            Map<DisplayPosition, DisplayMeta> displays = new EnumMap<>(DisplayPosition.class);
            for (Map.Entry<String, Object> entry : displayMap.entrySet()) {
                try {
                    DisplayPosition displayPosition = DisplayPosition.valueOf(entry.getKey().toUpperCase(Locale.ENGLISH));
                    if (entry.getValue() instanceof Map<?,?> metaMap) {
                        displays.put(displayPosition, DisplayMeta.fromMap(MiscUtils.castToMap(metaMap, false)));
                    }
                } catch (IllegalArgumentException e) {
                    throw new LocalizedResourceConfigException("warning.config.model.generation.invalid_display_position", e, entry.getKey(), EnumUtils.toString(DisplayPosition.values()));
                }
            }
            b.displays(displays);
        });
        BUILDER_FUNCTIONS.put("gui-light", (b, data) -> {
            String guiLightStr = String.valueOf(data);
            try {
                GuiLight guiLight = GuiLight.valueOf(guiLightStr.toUpperCase(Locale.ENGLISH));
                b.guiLight(guiLight);
            } catch (IllegalArgumentException e) {
                throw new LocalizedResourceConfigException("warning.config.model.generation.invalid_gui_light", e, guiLightStr, EnumUtils.toString(GuiLight.values()));
            }
        });
        BUILDER_FUNCTIONS.put("ambient-occlusion", (b, data) -> {
           b.ambientOcclusion(ResourceConfigUtils.getAsBoolean(data, "ambient-occlusion"));
        });
        BUILDER_FUNCTIONS.put("parent", (b, data) -> {
            String parentModelPath = data.toString();
            b.parentModelPath(parentModelPath);
        });
    }

    @NotNull
    private final Key path;
    @NotNull
    private final String parentModelPath;
    @Nullable
    private final Map<String, String> texturesOverride;
    @Nullable
    private final Map<DisplayPosition, DisplayMeta> displays;
    @Nullable
    private final GuiLight guiLight;
    @Nullable
    private final Boolean ambientOcclusion;
    @Nullable
    private JsonObject cachedModel;

    public ModelGeneration(@NotNull Key path,
                           @NotNull String parentModelPath,
                           @Nullable Map<String, String> texturesOverride,
                           @Nullable Map<DisplayPosition, DisplayMeta> displays,
                           @Nullable GuiLight guiLight,
                           @Nullable Boolean ambientOcclusion) {
        this.path = path;
        this.parentModelPath = parentModelPath;
        this.texturesOverride = texturesOverride;
        this.displays = displays;
        this.guiLight = guiLight;
        this.ambientOcclusion = ambientOcclusion;
    }

    public static ModelGeneration of(Key path, Map<String, Object> map) {
        Builder builder = builder().path(path);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Optional.ofNullable(BUILDER_FUNCTIONS.get(entry.getKey())).ifPresent(it -> it.accept(builder, entry.getValue()));
        }
        return builder.build();
    }

    @Nullable
    public Map<String, String> texturesOverride() {
        return texturesOverride;
    }

    public Key path() {
        return path;
    }

    public String parentModelPath() {
        return parentModelPath;
    }

    @Nullable
    public Map<DisplayPosition, DisplayMeta> displays() {
        return displays;
    }

    @Nullable
    public GuiLight guiLight() {
        return guiLight;
    }

    @Nullable
    public Boolean ambientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public JsonObject get() {
        if (this.cachedModel == null) {
            this.cachedModel = this.getCachedModel();
        }
        return this.cachedModel;
    }

    private JsonObject getCachedModel() {
        JsonObject model = new JsonObject();
        model.addProperty("parent", this.parentModelPath);
        if (this.texturesOverride != null) {
            JsonObject textures = new JsonObject();
            for (Map.Entry<String, String> entry : this.texturesOverride.entrySet()) {
                textures.addProperty(entry.getKey(), entry.getValue());
            }
            model.add("textures", textures);
        }
        if (this.displays != null) {
            JsonObject displays = new JsonObject();
            for (Map.Entry<DisplayPosition, DisplayMeta> entry : this.displays.entrySet()) {
                JsonObject displayMetadata = new JsonObject();
                DisplayMeta meta = entry.getValue();
                if (meta.rotation() != null)
                    displayMetadata.add("rotation", vectorToJsonArray(meta.rotation()));
                if (meta.translation() != null)
                    displayMetadata.add("translation", vectorToJsonArray(meta.translation()));
                if (meta.scale() != null)
                    displayMetadata.add("scale", vectorToJsonArray(meta.scale()));
                displays.add(entry.getKey().name().toLowerCase(Locale.ENGLISH), displayMetadata);
            }
            model.add("display", displays);
        }
        if (this.guiLight != null) {
            model.addProperty("gui_light", this.guiLight.name().toLowerCase(Locale.ENGLISH));
        }
        return model;
    }

    private JsonArray vectorToJsonArray(Vector3f vector) {
        JsonArray array = new JsonArray();
        array.add(vector.x());
        array.add(vector.y());
        array.add(vector.z());
        return array;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelGeneration that = (ModelGeneration) o;
        return this.path.equals(that.path) && parentModelPath.equals(that.parentModelPath) && Objects.equals(texturesOverride, that.texturesOverride)
                && Objects.equals(displays, that.displays) && Objects.equals(ambientOcclusion, that.ambientOcclusion) && Objects.equals(guiLight, that.guiLight);
    }

    @Override
    public int hashCode() {
        int result = this.path.hashCode();
        result = 31 * result + this.parentModelPath.hashCode();
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Key path;
        private String parentModelPath;
        @Nullable
        private Map<String, String> texturesOverride;
        @Nullable
        private Map<DisplayPosition, DisplayMeta> displays;
        @Nullable
        private GuiLight guiLight;
        @Nullable
        private Boolean ambientOcclusion;

        public Builder() {}

        public Builder path(Key key) {
            this.path = key;
            return this;
        }

        public Builder parentModelPath(String parentModelPath) {
            this.parentModelPath = parentModelPath;
            return this;
        }

        public Builder texturesOverride(Map<String, String> texturesOverride) {
            this.texturesOverride = texturesOverride;
            return this;
        }

        public Builder displays(Map<DisplayPosition, DisplayMeta> displays) {
            this.displays = displays;
            return this;
        }

        public Builder guiLight(GuiLight guiLight) {
            this.guiLight = guiLight;
            return this;
        }

        public Builder ambientOcclusion(Boolean ambientOcclusion) {
            this.ambientOcclusion = ambientOcclusion;
            return this;
        }

        public ModelGeneration build() {
            if (this.parentModelPath == null) {
                throw new LocalizedResourceConfigException("warning.config.model.generation.missing_parent");
            }
            return new ModelGeneration(Objects.requireNonNull(this.path, "path should be nonnull"), this.parentModelPath, this.texturesOverride, this.displays, this.guiLight, this.ambientOcclusion);
        }
    }
}
