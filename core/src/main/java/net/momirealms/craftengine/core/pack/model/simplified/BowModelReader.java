package net.momirealms.craftengine.core.pack.model.simplified;

import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ConditionItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.RangeDispatchItemModel;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.UseDurationRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BowModelReader implements SimplifiedModelReader {
    public static final BowModelReader INSTANCE = new BowModelReader();
    private static final Key BOW = Key.of("item/bow");
    private static final Key BOW_PULLING_0 = Key.of("item/bow_pulling_0");
    private static final Key BOW_PULLING_1 = Key.of("item/bow_pulling_1");
    private static final Key BOW_PULLING_2 = Key.of("item/bow_pulling_2");

    private BowModelReader() {}

    @Override
    @NotNull
    public ItemModel read(ConfigValue textureValue, Optional<ConfigValue> optionalModelValue, Key id) {
        List<Key> textures = textureValue.getAsFixedSizeList(4, ConfigValue::getAsAssetPath);
        List<Key> models = optionalModelValue.map(it -> it.getAsFixedSizeList(4, ConfigValue::getAsAssetPath)).orElse(null);
        boolean autoModel = models == null;
        return new ConditionItemModel(
                UsingItemConditionProperty.INSTANCE,
                new RangeDispatchItemModel(
                        new UseDurationRangeDispatchProperty(false),
                        0.05f,
                        Map.of(
                                0.65f, new BaseItemModel(
                                        autoModel ? Key.of(id.namespace(), "item/" + id.value() + "_pulling_1") : models.get(1),
                                        List.of(),
                                        ModelGeneration.builder()
                                                .parentModelPath(BOW_PULLING_1)
                                                .texturesOverride(Map.of("layer0", textures.get(2).asMinimalString()))
                                                .build()
                                ),
                                0.9f, new BaseItemModel(
                                        autoModel ? Key.of(id.namespace(), "item/" + id.value() + "_pulling_2") : models.get(1),
                                        List.of(),
                                        ModelGeneration.builder()
                                                .parentModelPath(BOW_PULLING_2)
                                                .texturesOverride(Map.of("layer0", textures.get(3).asMinimalString()))
                                                .build()
                                )
                        ), new BaseItemModel(
                                autoModel ? Key.of(id.namespace(), "item/" + id.value() + "_pulling_0") : models.get(1),
                                List.of(),
                                ModelGeneration.builder()
                                        .parentModelPath(BOW_PULLING_0)
                                        .texturesOverride(Map.of("layer0", textures.get(1).asMinimalString()))
                                        .build()
                        )
                ),
                new BaseItemModel(
                        autoModel ? Key.of(id.namespace(), "item/" + id.value()) : models.get(0),
                        List.of(),
                        ModelGeneration.builder()
                                .parentModelPath(BOW)
                                .texturesOverride(Map.of("layer0", textures.get(0).asMinimalString()))
                                .build()
                )
        );
    }

    @Override
    public ItemModel read(ConfigValue modelValue) {
        List<Key> models = modelValue.getAsFixedSizeList(4, ConfigValue::getAsAssetPath);
        return new ConditionItemModel(
                UsingItemConditionProperty.INSTANCE,
                new RangeDispatchItemModel(
                        new UseDurationRangeDispatchProperty(false),
                        0.05f,
                        Map.of(
                                0.65f, new BaseItemModel(models.get(2)),
                                0.9f, new BaseItemModel(models.get(3))
                        ),
                        new BaseItemModel(models.get(1))
                ),
                new BaseItemModel(models.get(0))
        );
    }
}
