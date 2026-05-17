package net.momirealms.craftengine.core.pack.model.generation;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayMeta;
import net.momirealms.craftengine.core.pack.model.generation.display.DisplayPosition;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.EnumUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VectorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public final class ModelGeneration implements Supplier<JsonObject> {
    @NotNull
    private final Key parentModelPath;
    @Nullable
    private final Map<String, String> texturesOverride;
    @Nullable
    private final Map<DisplayPosition, DisplayMeta> displays;
    @Nullable
    private final GuiLight guiLight;
    @Nullable
    private JsonObject cachedModel;

    public ModelGeneration(@NotNull Key parentModelPath,
                           @Nullable Map<String, String> texturesOverride,
                           @Nullable Map<DisplayPosition, DisplayMeta> displays,
                           @Nullable GuiLight guiLight) {
        this.parentModelPath = parentModelPath;
        this.texturesOverride = texturesOverride;
        this.displays = displays;
        this.guiLight = guiLight;
    }

    private static final String[] GUI_LIGHT = new String[]{"gui_light", "gui-light"};

    public static ModelGeneration of(ConfigSection section) {
        return builder()
                .parentModelPath(section.getNonNullIdentifier("parent"))
                .guiLight(section.getEnum(GUI_LIGHT, GuiLight.class))
                .displays(section.getValue("display", v -> {
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
                .texturesOverride(section.getValue("textures", v -> {
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

    @Override
    public JsonObject get() {
        if (this.cachedModel == null) {
            this.cachedModel = this.getCachedModel();
        }
        return this.cachedModel;
    }

    private JsonObject getCachedModel() {
        JsonObject model = new JsonObject();
        model.addProperty("parent", this.parentModelPath.asMinimalString());
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
                    displayMetadata.add("rotation", VectorUtils.toJson(meta.rotation()));
                if (meta.translation() != null)
                    displayMetadata.add("translation", VectorUtils.toJson(meta.translation()));
                if (meta.scale() != null)
                    displayMetadata.add("scale", VectorUtils.toJson(meta.scale()));
                displays.add(entry.getKey().name().toLowerCase(Locale.ROOT), displayMetadata);
            }
            model.add("display", displays);
        }
        if (this.guiLight != null) {
            model.addProperty("gui_light", this.guiLight.name().toLowerCase(Locale.ROOT));
        }
        return model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelGeneration that = (ModelGeneration) o;
        return parentModelPath.equals(that.parentModelPath) && Objects.equals(texturesOverride, that.texturesOverride)
                && Objects.equals(displays, that.displays) && Objects.equals(guiLight, that.guiLight);
    }

    @Override
    public int hashCode() {
        int i = this.parentModelPath.hashCode();
        if (this.texturesOverride != null) {
            i += 31 * this.texturesOverride.hashCode();
        }
        return i;
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

        public ModelGeneration build() {
            return new ModelGeneration(this.parentModelPath, this.texturesOverride, this.displays, this.guiLight);
        }
    }
}
