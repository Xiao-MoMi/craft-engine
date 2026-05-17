package net.momirealms.craftengine.core.pack.model.simplified;

import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ConditionItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.condition.BrokenConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.condition.ConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.condition.RodCastConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ConditionModelReader implements SimplifiedModelReader {
    public static final ConditionModelReader FISHING_ROD = new ConditionModelReader(Key.of("item/fishing_rod"), RodCastConditionProperty.INSTANCE, "_cast");
    public static final ConditionModelReader ELYTRA = new ConditionModelReader(Key.of("item/generated"), BrokenConditionProperty.INSTANCE, "_broken");
    public static final ConditionModelReader SHIELD = new ConditionModelReader(Key.of("item/generated"), UsingItemConditionProperty.INSTANCE, "_blocking");
    private final Key model;
    private final ConditionProperty property;
    private final String suffix;

    private ConditionModelReader(Key model, ConditionProperty property, String suffix) {
        this.model = model;
        this.property = property;
        this.suffix = suffix;
    }

    @Override
    public ItemModel read(ConfigValue textureValue, Optional<ConfigValue> optionalModelValue, Key id) {
        List<Key> textures = textureValue.getAsFixedSizeList(2, ConfigValue::getAsAssetPath);
        List<Key> models = optionalModelValue.map(it -> it.getAsFixedSizeList(2, ConfigValue::getAsAssetPath)).orElse(null);
        boolean autoModel = models == null;
        return new ConditionItemModel(
                this.property,
                new BaseItemModel(
                        autoModel ? Key.of(id.namespace(), "item/" + id.value() + this.suffix) : models.getLast(),
                        List.of(),
                        ModelGeneration.builder()
                                .parentModelPath(this.model)
                                .texturesOverride(Map.of("layer0", textures.getLast().asMinimalString()))
                                .build()
                ),
                new BaseItemModel(
                        autoModel ? Key.of(id.namespace(), "item/" + id.value()) : models.getFirst(),
                        List.of(),
                        ModelGeneration.builder()
                                .parentModelPath(this.model)
                                .texturesOverride(Map.of("layer0", textures.getFirst().asMinimalString()))
                                .build()
                )
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
}
