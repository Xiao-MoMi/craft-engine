package net.momirealms.craftengine.core.pack.model.generation;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.ModelJsonKeys;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayMeta;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.util.EnumUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public final class ModelGeneration implements Supplier<JsonObject> {
    @Nullable
    private final Key parentModelPath;
    @Nullable
    private final Map<String, String> texturesOverride;
    @Nullable
    private final Map<DisplayPosition, DisplayMeta> displays;
    @Nullable
    private final GuiLight guiLight;
    @Nullable
    private final Boolean ambientOcclusion;
    @Nullable
    private final JsonObject rawModel;
    @Nullable
    private final Map<Key, byte[]> rawTextures;
    private final Map<Key, JsonObject> rawTextureMetadata;
    @Nullable
    private JsonObject cachedModel;

    public ModelGeneration(@NotNull Key parentModelPath,
                           @Nullable Map<String, String> texturesOverride,
                           @Nullable Map<DisplayPosition, DisplayMeta> displays,
                           @Nullable GuiLight guiLight,
                           @Nullable Boolean ambientOcclusion) {
        this.parentModelPath = parentModelPath;
        this.texturesOverride = texturesOverride;
        this.displays = displays;
        this.guiLight = guiLight;
        this.ambientOcclusion = ambientOcclusion;
        this.rawModel = null;
        this.rawTextures = null;
        this.rawTextureMetadata = Map.of();
    }

    private ModelGeneration(@NotNull JsonObject rawModel, @Nullable Map<Key, byte[]> rawTextures, Map<Key, JsonObject> rawTextureMetadata) {
        this.parentModelPath = null;
        this.texturesOverride = null;
        this.displays = null;
        this.guiLight = null;
        this.ambientOcclusion = null;
        this.rawModel = rawModel;
        this.rawTextures = rawTextures;
        this.rawTextureMetadata = rawTextureMetadata;
    }

    public static ModelGeneration raw(JsonObject model, Map<Key, byte[]> textures) {
        return raw(model, textures, Map.of());
    }

    public static ModelGeneration raw(JsonObject model, Map<Key, byte[]> textures, Map<Key, JsonObject> textureMetadata) {
        return new ModelGeneration(model, textures, textureMetadata);
    }

    public Map<Key, JsonObject> rawTextureMetadata() {
        return this.rawTextureMetadata;
    }

    @NotNull
    public Map<Key, byte[]> rawTextures() {
        return this.rawTextures != null ? this.rawTextures : Map.of();
    }

    private static final String[] GUI_LIGHT = ConfigKeys.of(ModelJsonKeys.GUI_LIGHT);
    private static final String[] AMBIENT_OCCLUSION = ConfigKeys.of(ModelJsonKeys.AMBIENT_OCCLUSION + "|ambient_occlusion");

    public static ModelGeneration of(ConfigSection section) {
        return builder()
                .parentModelPath(section.getNonNullIdentifier(ModelJsonKeys.PARENT))
                .guiLight(section.getEnum(GUI_LIGHT, GuiLight.class))
                .ambientOcclusion(section.getValue(AMBIENT_OCCLUSION, v -> v.getAsBoolean()))
                .displays(section.getValue(ModelJsonKeys.DISPLAY, v -> {
                    Map<DisplayPosition, DisplayMeta> displays = new EnumMap<>(DisplayPosition.class);
                    ConfigSection displaySection = v.getAsSection();
                    for (String displayType : displaySection.keySet()) {
                        DisplayPosition position;
                        try {
                            position = DisplayPosition.valueOf(displayType.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            throw new KnownResourceException(ConfigConstants.PARSE_ENUM_FAILED, displaySection.path(), displayType, EnumUtils.toString(DisplayPosition.values()));
                        }
                        displays.put(position, displaySection.getValue(displayType, a -> DisplayMeta.fromConfig(a.getAsSection())));
                    }
                    return displays;
                }))
                .texturesOverride(section.getValue(ModelJsonKeys.TEXTURES, v -> {
                    ConfigSection texturesSection = v.getAsSection();
                    Map<String, String> texturesOverride = new LinkedHashMap<>();
                    for (String key : texturesSection.keySet()) {
                        ConfigValue configValue = texturesSection.getValue(key);
                        assert configValue != null;
                        String value = configValue.getAsString();
                        if (value.charAt(0) == '#') {
                            texturesOverride.put(key, value);
                        } else {
                            texturesOverride.put(key, configValue.getAsAssetPath().asMinimalString());
                        }
                    }
                    return texturesOverride;
                }))
                .build();
    }

    @Nullable
    public Map<String, String> texturesOverride() {
        return this.texturesOverride;
    }

    @Nullable
    public Key parentModelPath() {
        return this.parentModelPath;
    }

    @Nullable
    public Map<DisplayPosition, DisplayMeta> displays() {
        return this.displays;
    }

    @Nullable
    public GuiLight guiLight() {
        return this.guiLight;
    }

    @Nullable
    public Boolean ambientOcclusion() {
        return this.ambientOcclusion;
    }

    @Override
    public JsonObject get() {
        if (this.rawModel != null) {
            return this.rawModel;
        }
        if (this.cachedModel == null) {
            this.cachedModel = this.getCachedModel();
        }
        return this.cachedModel;
    }

    private JsonObject getCachedModel() {
        JsonObject model = new JsonObject();
        model.addProperty(ModelJsonKeys.PARENT, this.parentModelPath.asMinimalString());
        if (this.texturesOverride != null) {
            JsonObject textures = new JsonObject();
            for (Map.Entry<String, String> entry : this.texturesOverride.entrySet()) {
                textures.addProperty(entry.getKey(), entry.getValue());
            }
            model.add(ModelJsonKeys.TEXTURES, textures);
        }
        if (this.displays != null) {
            JsonObject displays = new JsonObject();
            for (Map.Entry<DisplayPosition, DisplayMeta> entry : this.displays.entrySet()) {
                JsonObject displayMetadata = new JsonObject();
                DisplayMeta meta = entry.getValue();
                if (meta.rotation() != null)
                    displayMetadata.add(ModelJsonKeys.ROTATION, VectorUtils.toJson(meta.rotation()));
                if (meta.translation() != null)
                    displayMetadata.add(ModelJsonKeys.TRANSLATION, VectorUtils.toJson(meta.translation()));
                if (meta.scale() != null)
                    displayMetadata.add(ModelJsonKeys.SCALE, VectorUtils.toJson(meta.scale()));
                displays.add(entry.getKey().name().toLowerCase(Locale.ROOT), displayMetadata);
            }
            model.add(ModelJsonKeys.DISPLAY, displays);
        }
        if (this.guiLight != null) {
            model.addProperty(ModelJsonKeys.GUI_LIGHT, this.guiLight.name().toLowerCase(Locale.ROOT));
        }
        if (this.ambientOcclusion != null) {
            model.addProperty(ModelJsonKeys.AMBIENT_OCCLUSION, this.ambientOcclusion);
        }
        return model;
    }

    public boolean isSameJsonModel(ModelGeneration that) {
        return Objects.equals(parentModelPath, that.parentModelPath) && Objects.equals(texturesOverride, that.texturesOverride)
                && Objects.equals(displays, that.displays) && Objects.equals(guiLight, that.guiLight)
                && Objects.equals(ambientOcclusion, that.ambientOcclusion) && Objects.equals(rawModel, that.rawModel);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Key parentModelPath;
        @Nullable
        private Map<String, String> texturesOverride;
        @Nullable
        private Map<DisplayPosition, DisplayMeta> displays;
        @Nullable
        private GuiLight guiLight;
        @Nullable
        private Boolean ambientOcclusion;

        public Builder() {
        }

        public Builder parentModelPath(Key parentModelPath) {
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
            return new ModelGeneration(this.parentModelPath, this.texturesOverride, this.displays, this.guiLight, this.ambientOcclusion);
        }
    }
}
