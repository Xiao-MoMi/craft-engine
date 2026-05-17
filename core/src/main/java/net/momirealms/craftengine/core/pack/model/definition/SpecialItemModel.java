package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.definition.special.SpecialModel;
import net.momirealms.craftengine.core.pack.model.definition.special.SpecialModels;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class SpecialItemModel implements ItemModel {
    public static final ItemModelFactory<SpecialItemModel> FACTORY = new Factory();
    public static final ItemModelReader<SpecialItemModel> READER = new Reader();
    private final SpecialModel specialModel;
    private final Key base;
    private final ModelGeneration modelGeneration;
    private final Transformation transformation;

    public SpecialItemModel(@NotNull SpecialModel specialModel, @NotNull Key base, @Nullable ModelGeneration generation, @Nullable Transformation transformation) {
        this.specialModel = specialModel;
        this.base = base;
        this.modelGeneration = generation;
        this.transformation = transformation;
    }

    public SpecialItemModel(@NotNull SpecialModel specialModel, @NotNull Key base, @Nullable ModelGeneration generation) {
        this(specialModel, base, generation, null);
    }

    public SpecialItemModel(@NotNull SpecialModel specialModel, @NotNull Key base, @Nullable Transformation transformation) {
        this(specialModel, base, null, transformation);
    }

    public SpecialItemModel(@NotNull SpecialModel specialModel, @NotNull Key base) {
        this(specialModel, base, null, null);
    }

    @NotNull
    public SpecialModel specialModel() {
        return this.specialModel;
    }

    @Nullable
    public Transformation transformation() {
        return this.transformation;
    }

    @Nullable
    public ModelGeneration modelGeneration() {
        return this.modelGeneration;
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
    public void prepareModelGeneration(Consumer<ModelGenerationHolder> consumer) {
        if (this.modelGeneration != null) {
            consumer.accept(new ModelGenerationHolder(this.base, this.modelGeneration));
        }
    }

    @Override
    public void gatherRevisions(Consumer<Revision> consumer) {
        this.specialModel.collectRevision(consumer);
    }

    private static class Factory implements ItemModelFactory<SpecialItemModel> {
        private static final String[] BASE = new String[] {"base", "path"};

        @Override
        public SpecialItemModel create(ConfigSection section) {
            Key base = section.getNonNullIdentifier(BASE);
            ConfigSection generation = section.getSection("generation");
            ModelGeneration modelGeneration = null;
            if (generation != null) {
                modelGeneration = ModelGeneration.of(generation);
            }
            return new SpecialItemModel(
                    SpecialModels.fromConfig(section.getNonNullSection("model")),
                    base,
                    modelGeneration,
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
