package net.momirealms.craftengine.core.pack.model.simplified.item;

import net.momirealms.craftengine.core.pack.model.bbmodel.BBModelConverter;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ConditionItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.condition.BrokenConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.condition.RodCastConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class ConditionItemModelReader implements SimplifiedItemModelReader {
    public static final ConditionItemModelReader FISHING_ROD = new ConditionItemModelReader(Key.of("item/fishing_rod"), RodCastConditionProperty.INSTANCE, "_cast");
    public static final ConditionItemModelReader ELYTRA = new ConditionItemModelReader(Key.of("item/generated"), BrokenConditionProperty.INSTANCE, "_broken");
    public static final ConditionItemModelReader SHIELD = new ConditionItemModelReader(Key.of("item/generated"), UsingItemConditionProperty.INSTANCE, "_blocking");
    private final Key model;
    private final ConditionProperty property;
    private final String suffix;

    private ConditionItemModelReader(Key model, ConditionProperty property, String suffix) {
        this.model = model;
        this.property = property;
        this.suffix = suffix;
    }

    @Override
    public ItemModel read(ConfigValue textureValue, Optional<ConfigValue> optionalModelValue, Key id, Consumer<ModelGenerationHolder> generations) {
        List<Key> textures = textureValue.getAsFixedSizeList(2, ConfigValue::getAsAssetPath);
        List<Key> models = optionalModelValue.map(it -> it.getAsFixedSizeList(2, ConfigValue::getAsAssetPath)).orElse(null);
        return build(
                models != null ? models.getFirst() : Key.of(id.namespace(), "item/" + id.value()),
                ModelGeneration.builder()
                        .parentModelPath(this.model)
                        .texturesOverride(Map.of("layer0", textures.getFirst().asMinimalString()))
                        .build(),
                models != null ? models.getLast() : Key.of(id.namespace(), "item/" + id.value() + this.suffix),
                ModelGeneration.builder()
                        .parentModelPath(this.model)
                        .texturesOverride(Map.of("layer0", textures.getLast().asMinimalString()))
                        .build(),
                generations
        );
    }

    @Override
    public ItemModel read(ConfigValue modelValue) {
        List<Key> models = modelValue.getAsFixedSizeList(2, ConfigValue::getAsAssetPath);
        return new ConditionItemModel(
                this.property,
                new BaseItemModel(models.getLast()),
                new BaseItemModel(models.getFirst())
        );
    }

    @Override
    public int modelCount() {
        return 2;
    }

    @Override
    public ItemModel buildFromBlueprints(List<BBModelConverter.Converted> blueprints, Consumer<ModelGenerationHolder> generations) {
        BBModelConverter.Converted idle = blueprints.get(0);
        BBModelConverter.Converted special = blueprints.get(1);
        return build(
                idle.model(), ModelGeneration.raw(idle.json(), idle.textures(), idle.textureMetadata()),
                special.model(), ModelGeneration.raw(special.json(), special.textures(), special.textureMetadata()), generations
        );
    }

    private ItemModel build(Key idlePath, ModelGeneration idleGeneration, Key specialPath, ModelGeneration specialGeneration, Consumer<ModelGenerationHolder> generations) {
        generations.accept(new ModelGenerationHolder(idlePath, idleGeneration));
        generations.accept(new ModelGenerationHolder(specialPath, specialGeneration));
        return new ConditionItemModel(
                this.property,
                new BaseItemModel(specialPath),
                new BaseItemModel(idlePath)
        );
    }
}
