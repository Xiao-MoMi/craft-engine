package net.momirealms.craftengine.core.pack.model.simplified.item;

import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.pack.model.bbmodel.BBModelConverter;
import net.momirealms.craftengine.core.pack.model.definition.*;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CrossBowPullingRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Map2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class CrossbowItemModelReader implements SimplifiedItemModelReader {
    public static final CrossbowItemModelReader INSTANCE = new CrossbowItemModelReader();
    private static final Key[] PARENTS = new Key[] {
            Key.of("item/crossbow"),
            Key.of("item/crossbow_pulling_0"),
            Key.of("item/crossbow_pulling_1"),
            Key.of("item/crossbow_pulling_2"),
            Key.of("item/crossbow_arrow"),
            Key.of("item/crossbow_firework")
    };
    private static final String[] AUTO_SUFFIXES = new String[]{"", "_pulling_0", "_pulling_1", "_pulling_2", "_arrow", "_firework"};

    private CrossbowItemModelReader() {}

    @Override
    public ItemModel read(ConfigValue textureValue, Optional<ConfigValue> optionalModelValue, Key id, Consumer<ModelGenerationHolder> generations) {
        List<Key> textures = textureValue.getAsFixedSizeList(6, ConfigValue::getAsAssetPath);
        List<Key> models = optionalModelValue.map(it -> it.getAsFixedSizeList(6, ConfigValue::getAsAssetPath)).orElse(null);
        Key[] paths = new Key[6];
        for (int i = 0; i < 6; i++) {
            paths[i] = models != null ? models.get(i) : Key.of(id.namespace(), "item/" + id.value() + AUTO_SUFFIXES[i]);
            ModelGeneration generation = ModelGeneration.builder()
                    .parentModelPath(PARENTS[i])
                    .texturesOverride(Map.of("layer0", textures.get(i).asMinimalString()))
                    .build();
            generations.accept(new ModelGenerationHolder(paths[i], generation));
        }
        return build(paths);
    }

    @Override
    public ItemModel read(ConfigValue modelValue) {
        List<Key> models = modelValue.getAsFixedSizeList(6, ConfigValue::getAsAssetPath);
        return new SelectItemModel(
                ChargeTypeSelectProperty.INSTANCE,
                Map2.of(
                        Either.left(new JsonPrimitive("arrow")), new BaseItemModel(models.get(4)),
                        Either.left(new JsonPrimitive("rocket")), new BaseItemModel(models.get(5))
                ),
                new ConditionItemModel(
                        UsingItemConditionProperty.INSTANCE,
                        new RangeDispatchItemModel(
                                CrossBowPullingRangeDispatchProperty.INSTANCE,
                                1f,
                                Map2.of(
                                        0.58f, new BaseItemModel(models.get(2)),
                                        1.0f, new BaseItemModel(models.get(3))
                                ),
                                new BaseItemModel(models.get(1))
                        ),
                        new BaseItemModel(models.get(0))
                )
        );
    }

    @Override
    public int modelCount() {
        return 6;
    }

    @Override
    public ItemModel buildFromBlueprints(List<BBModelConverter.Converted> blueprints, Consumer<ModelGenerationHolder> generations) {
        Key[] paths = new Key[6];
        for (int i = 0; i < 6; i++) {
            BBModelConverter.Converted converted = blueprints.get(i);
            paths[i] = converted.model();
            generations.accept(new ModelGenerationHolder(paths[i], ModelGeneration.raw(converted.json(), converted.textures(), converted.textureMetadata())));
        }
        return build(paths);
    }

    private ItemModel build(Key[] paths) {
        return new SelectItemModel(
                ChargeTypeSelectProperty.INSTANCE,
                Map2.of(
                        Either.left(new JsonPrimitive("arrow")), new BaseItemModel(paths[4]),
                        Either.left(new JsonPrimitive("rocket")), new BaseItemModel(paths[5])
                ),
                new ConditionItemModel(
                        UsingItemConditionProperty.INSTANCE,
                        new RangeDispatchItemModel(
                                CrossBowPullingRangeDispatchProperty.INSTANCE,
                                1f,
                                Map2.of(
                                        0.58f, new BaseItemModel(paths[2]),
                                        1.0f, new BaseItemModel(paths[3])
                                ),
                                new BaseItemModel(paths[1])
                        ),
                        new BaseItemModel(paths[0])
                )
        );
    }
}
