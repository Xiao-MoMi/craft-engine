package net.momirealms.craftengine.core.pack.model.simplified;

import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.pack.model.definition.*;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CrossBowPullingRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CrossbowModelReader implements SimplifiedModelReader {
    public static final CrossbowModelReader INSTANCE = new CrossbowModelReader();
    private static final Key CROSSBOW = Key.of("item/crossbow");
    private static final Key CROSSBOW_PULLING_0 = Key.of("item/crossbow_pulling_0");
    private static final Key CROSSBOW_PULLING_1 = Key.of("item/crossbow_pulling_1");
    private static final Key CROSSBOW_PULLING_2 = Key.of("item/crossbow_pulling_2");
    private static final Key CROSSBOW_ARROW = Key.of("item/crossbow_arrow");
    private static final Key CROSSBOW_FIREWORK = Key.of("item/crossbow_firework");

    private CrossbowModelReader() {}

    @Override
    public ItemModel read(ConfigValue textureValue, Optional<ConfigValue> optionalModelValue, Key id) {
        List<Key> textures = textureValue.getAsFixedSizeList(6, ConfigValue::getAsAssetPath);
        List<Key> models = optionalModelValue.map(it -> it.getAsFixedSizeList(6, ConfigValue::getAsAssetPath)).orElse(null);
        boolean autoModel = models == null;
        return new ConditionItemModel(
                UsingItemConditionProperty.INSTANCE,
                new RangeDispatchItemModel(
                        CrossBowPullingRangeDispatchProperty.INSTANCE,
                        1f,
                        Map.of(
                                0.58f, new BaseItemModel(
                                        autoModel ? Key.of(id.namespace(), "item/" + id.value() + "_pulling_1") : models.get(2),
                                        List.of(),
                                        ModelGeneration.builder()
                                                .parentModelPath(CROSSBOW_PULLING_1)
                                                .texturesOverride(Map.of("layer0", textures.get(2).asMinimalString()))
                                                .build()
                                ),
                                1.0f, new BaseItemModel(
                                        autoModel ? Key.of(id.namespace(), "item/" + id.value() + "_pulling_2") : models.get(3),
                                        List.of(),
                                        ModelGeneration.builder()
                                                .parentModelPath(CROSSBOW_PULLING_2)
                                                .texturesOverride(Map.of("layer0", textures.get(3).asMinimalString()))
                                                .build()
                                )
                        ),
                        new BaseItemModel(
                                autoModel ? Key.of(id.namespace(), "item/" + id.value() + "_pulling_0") : models.get(1),
                                List.of(),
                                ModelGeneration.builder()
                                        .parentModelPath(CROSSBOW_PULLING_0)
                                        .texturesOverride(Map.of("layer0", textures.get(1).asMinimalString()))
                                        .build()
                        )
                ),
                new SelectItemModel(
                        ChargeTypeSelectProperty.INSTANCE,
                        Map.of(
                                Either.left(new JsonPrimitive("arrow")), new BaseItemModel(
                                        autoModel ? Key.of(id.namespace(), "item/" + id.value() + "_arrow") : models.get(4),
                                        List.of(),
                                        ModelGeneration.builder()
                                                .parentModelPath(CROSSBOW_ARROW)
                                                .texturesOverride(Map.of("layer0", textures.get(4).asMinimalString()))
                                                .build()
                                ),
                                Either.left(new JsonPrimitive("rocket")), new BaseItemModel(
                                        autoModel ? Key.of(id.namespace(), "item/" + id.value() + "_firework") : models.get(5),
                                        List.of(),
                                        ModelGeneration.builder()
                                                .parentModelPath(CROSSBOW_FIREWORK)
                                                .texturesOverride(Map.of("layer0", textures.get(5).asMinimalString()))
                                                .build()
                                )
                        ),
                        new BaseItemModel(
                                autoModel ? Key.of(id.namespace(), "item/" + id.value()) : models.get(0),
                                List.of(),
                                ModelGeneration.builder()
                                        .parentModelPath(CROSSBOW)
                                        .texturesOverride(Map.of("layer0", textures.get(0).asMinimalString()))
                                        .build()
                        )
                )
        );
    }

    @Override
    public ItemModel read(ConfigValue modelValue) {
        List<Key> models = modelValue.getAsFixedSizeList(6, ConfigValue::getAsAssetPath);
        return new ConditionItemModel(
                UsingItemConditionProperty.INSTANCE,
                new RangeDispatchItemModel(
                        CrossBowPullingRangeDispatchProperty.INSTANCE,
                        1f,
                        Map.of(
                                0.58f, new BaseItemModel(models.get(2)),
                                1.0f, new BaseItemModel(models.get(3))
                        ),
                        new BaseItemModel(models.get(1))
                ),
                new SelectItemModel(
                        ChargeTypeSelectProperty.INSTANCE,
                        Map.of(
                                Either.left(new JsonPrimitive("arrow")), new BaseItemModel(models.get(4)),
                                Either.left(new JsonPrimitive("rocket")), new BaseItemModel(models.get(5))
                        ),
                        new BaseItemModel(models.get(0))
                )
        );
    }
}
