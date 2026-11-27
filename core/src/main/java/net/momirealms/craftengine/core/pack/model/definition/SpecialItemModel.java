package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.model.bbmodel.BBModelConverter;
import net.momirealms.craftengine.core.pack.model.definition.special.SpecialModel;
import net.momirealms.craftengine.core.pack.model.definition.special.SpecialModels;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public final class SpecialItemModel implements ItemModel {
    public static final ItemModelFactory<SpecialItemModel> FACTORY = new Factory();
    public static final ItemModelReader<SpecialItemModel> READER = new Reader();
    private final SpecialModel specialModel;
    private final Key base;
    private final Transformation transformation;

    public SpecialItemModel(@NotNull SpecialModel specialModel, @NotNull Key base, @Nullable Transformation transformation) {
        this.specialModel = specialModel;
        this.base = base;
        this.transformation = transformation;
    }

    public SpecialItemModel(@NotNull SpecialModel specialModel, @NotNull Key base) {
        this(specialModel, base, null);
    }

    @NotNull
    public SpecialModel specialModel() {
        return this.specialModel;
    }

    @Nullable
    public Transformation transformation() {
        return this.transformation;
    }

    @NotNull
    public Key base() {
        return this.base;
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "special");
        json.add("model", this.specialModel.toJson(min, max));
        json.addProperty("base", this.base.asMinimalString());
        if (this.transformation != null && max.isAtOrAbove(MinecraftVersion.V26_1)) {
            json.add("transformation", this.transformation.toJson());
        }
        return json;
    }

    @Override
    public void gatherRevisions(Consumer<Revision> consumer) {
        if (this.transformation != null) {
            consumer.accept(Revisions.SINCE_26_1);
        }
        this.specialModel.collectRevision(consumer);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SpecialItemModel that)) {
            return false;
        }
        return Objects.equals(this.specialModel, that.specialModel)
                && Objects.equals(this.base, that.base)
                && Objects.equals(this.transformation, that.transformation);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.specialModel);
        result = 31 * result + Objects.hashCode(this.base);
        return 31 * result + Objects.hashCode(this.transformation);
    }

    private static class Factory implements ItemModelFactory<SpecialItemModel> {
        private static final String[] BASE = ConfigKeys.of("base|path");

        @Override
        public SpecialItemModel create(Pack pack, Path path, ConfigSection section, Consumer<ModelGenerationHolder> generations) {
            ConfigValue blueprintValue = section.getValue("blueprint");
            Key base;
            ModelGeneration modelGeneration;
            if (blueprintValue != null) {
                BBModelConverter.Converted converted = BBModelConverter.convert(pack, path, "item", section.getValue(BASE), blueprintValue);
                base = converted.model();
                modelGeneration = ModelGeneration.raw(converted.json(), converted.textures(), converted.textureMetadata());
            } else {
                ConfigValue baseValue = section.getNonNullValue(BASE, ConfigConstants.ARGUMENT_IDENTIFIER);
                base = baseValue.getAsIdentifier();
                ConfigSection generation = section.getSection("generation");
                modelGeneration = null;
                if (generation != null) {
                    modelGeneration = ModelGeneration.of(generation);
                }
            }
            if (modelGeneration != null) {
                generations.accept(new ModelGenerationHolder(base, modelGeneration));
            }
            return new SpecialItemModel(
                    SpecialModels.fromConfig(section.getNonNullSection("model")),
                    base,
                    section.getValue("transformation", Transformation::fromConfig)
            );
        }
    }

    private static class Reader implements ItemModelReader<SpecialItemModel> {

        @Override
        public SpecialItemModel read(JsonObject json) {
            return new SpecialItemModel(
                    SpecialModels.fromJson(json.getAsJsonObject("model")),
                    Key.of(json.get("base").getAsString()),
                    json.has("transformation") ? Transformation.fromJson(json.get("transformation")) : null
            );
        }
    }
}
