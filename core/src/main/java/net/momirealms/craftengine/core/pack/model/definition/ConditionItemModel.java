package net.momirealms.craftengine.core.pack.model.definition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperties;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.MinecraftVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        this.onTrue.gatherRevisions(consumer);
        this.onFalse.gatherRevisions(consumer);
    }

    @Override
    public void prepareModelGeneration(Consumer<ModelGenerationHolder> consumer) {
        this.onTrue.prepareModelGeneration(consumer);
        this.onFalse.prepareModelGeneration(consumer);
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

    private static class Factory implements ItemModelFactory<ConditionItemModel> {
        private static final String[] ON_TRUE = new String[] {"on_true", "on-true"};
        private static final String[] ON_FALSE = new String[] {"on_false", "on-false"};

        @Override
        public ConditionItemModel create(ConfigSection section) {
            return new ConditionItemModel(
                    ConditionProperties.fromConfig(section),
                    section.getNonNullValue(ON_TRUE, ConfigConstants.ARGUMENT_ITEM_MODEL_DEFINITION, ItemModels::fromConfig),
                    section.getNonNullValue(ON_FALSE, ConfigConstants.ARGUMENT_ITEM_MODEL_DEFINITION, ItemModels::fromConfig),
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
