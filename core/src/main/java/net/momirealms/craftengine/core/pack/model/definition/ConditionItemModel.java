package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperties;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.pack.revision.Revisions;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public final class ConditionItemModel implements ItemModel {
    public static final ItemModelFactory<ConditionItemModel> FACTORY = new Factory();
    public static final ItemModelReader<ConditionItemModel> READER = new Reader();
    private final ConditionProperty property;
    private final ItemModel onTrue;
    private final ItemModel onFalse;
    private final Transformation transformation;

    public ConditionItemModel(@NotNull ConditionProperty property, @NotNull ItemModel onTrue, @NotNull ItemModel onFalse, @Nullable Transformation transformation) {
        this.property = property;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
        this.transformation = transformation;
    }

    public ConditionItemModel(@NotNull ConditionProperty property, @NotNull ItemModel onTrue, @NotNull ItemModel onFalse) {
        this(property, onTrue, onFalse, null);
    }

    @Nullable
    public Transformation transformation() {
        return this.transformation;
    }

    @NotNull
    public ConditionProperty property() {
        return this.property;
    }

    @NotNull
    public ItemModel onTrue() {
        return this.onTrue;
    }

    @NotNull
    public ItemModel onFalse() {
        return this.onFalse;
    }

    @Override
    public void gatherRevisions(Consumer<Revision> consumer) {
        if (this.transformation != null) {
            consumer.accept(Revisions.SINCE_26_1);
        }
        this.onTrue.gatherRevisions(consumer);
        this.onFalse.gatherRevisions(consumer);
    }

    @Override
    public JsonObject toJson(MinecraftVersion min, MinecraftVersion max) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "condition");
        json.add("on_true", this.onTrue.toJson(min, max));
        json.add("on_false", this.onFalse.toJson(min, max));
        if (this.transformation != null && max.isAtOrAbove(MinecraftVersion.V26_1)) {
            json.add("transformation", this.transformation.toJson());
        }
        this.property.writeProperty(json);
        return json;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ConditionItemModel that)) {
            return false;
        }
        return Objects.equals(this.property, that.property)
                && Objects.equals(this.onTrue, that.onTrue)
                && Objects.equals(this.onFalse, that.onFalse)
                && Objects.equals(this.transformation, that.transformation);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.property);
        result = 31 * result + Objects.hashCode(this.onTrue);
        result = 31 * result + Objects.hashCode(this.onFalse);
        return 31 * result + Objects.hashCode(this.transformation);
    }

    private static class Factory implements ItemModelFactory<ConditionItemModel> {
        private static final String[] ON_TRUE = ConfigKeys.of("on_true");
        private static final String[] ON_FALSE = ConfigKeys.of("on_false");

        @Override
        public ConditionItemModel create(Pack pack, Path path, ConfigSection section, Consumer<ModelGenerationHolder> generations) {
            return new ConditionItemModel(
                    ConditionProperties.fromConfig(section),
                    section.getNonNullValue(ON_TRUE, ConfigConstants.ARGUMENT_ITEM_MODEL_DEFINITION, v -> ItemModels.fromConfig(pack, path, v, generations)),
                    section.getNonNullValue(ON_FALSE, ConfigConstants.ARGUMENT_ITEM_MODEL_DEFINITION, v -> ItemModels.fromConfig(pack, path, v, generations)),
                    section.getValue("transformation", Transformation::fromConfig)
            );
        }
    }

    private static class Reader implements ItemModelReader<ConditionItemModel> {

        @Override
        public ConditionItemModel read(JsonObject json) {
            return new ConditionItemModel(
                    ConditionProperties.fromJson(json),
                    ItemModels.fromJson(json.getAsJsonObject("on_true")),
                    ItemModels.fromJson(json.getAsJsonObject("on_false")),
                    json.has("transformation") ? Transformation.fromJson(json.get("transformation")) : null
            );
        }
    }
}
