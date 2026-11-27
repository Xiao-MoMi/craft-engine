package net.momirealms.craftengine.core.pack.model.simplified.item;

import net.momirealms.craftengine.core.pack.model.bbmodel.BBModelConverter;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ConditionItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.RangeDispatchItemModel;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.UseDurationRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Map2;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class BowItemModelReader implements SimplifiedItemModelReader {
    public static final BowItemModelReader INSTANCE = new BowItemModelReader();
    private static final Key[] PARENTS = new Key[] {
            Key.of("item/bow"),
            Key.of("item/bow_pulling_0"),
            Key.of("item/bow_pulling_1"),
            Key.of("item/bow_pulling_2")
    };
    private static final String[] AUTO_SUFFIXES = new String[]{"", "_pulling_0", "_pulling_1", "_pulling_2"};

    private BowItemModelReader() {}

    @Override
    @NotNull
    public ItemModel read(ConfigValue textureValue, Optional<ConfigValue> optionalModelValue, Key id, Consumer<ModelGenerationHolder> generations) {
        List<Key> textures = textureValue.getAsFixedSizeList(4, ConfigValue::getAsAssetPath);
        List<Key> models = optionalModelValue.map(it -> it.getAsFixedSizeList(4, ConfigValue::getAsAssetPath)).orElse(null);
        Key[] paths = new Key[4];
        for (int i = 0; i < 4; i++) {
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
        List<Key> models = modelValue.getAsFixedSizeList(4, ConfigValue::getAsAssetPath);
        return new ConditionItemModel(
                UsingItemConditionProperty.INSTANCE,
                new RangeDispatchItemModel(
                        new UseDurationRangeDispatchProperty(false),
                        0.05f,
                        Map2.of(
                                0.65f, new BaseItemModel(models.get(2)),
                                0.9f, new BaseItemModel(models.get(3))
                        ),
                        new BaseItemModel(models.get(1))
                ),
                new BaseItemModel(models.get(0))
        );
    }

    @Override
    public int modelCount() {
        return 4;
    }

    @Override
    public ItemModel buildFromBlueprints(List<BBModelConverter.Converted> blueprints, Consumer<ModelGenerationHolder> generations) {
        Key[] paths = new Key[4];
        for (int i = 0; i < 4; i++) {
            BBModelConverter.Converted converted = blueprints.get(i);
            paths[i] = converted.model();
            generations.accept(new ModelGenerationHolder(paths[i], ModelGeneration.raw(converted.json(), converted.textures(), converted.textureMetadata())));
        }
        return build(paths);
    }

    private ItemModel build(Key[] paths) {
        return new ConditionItemModel(
                UsingItemConditionProperty.INSTANCE,
                new RangeDispatchItemModel(
                        new UseDurationRangeDispatchProperty(false),
                        0.05f,
                        Map2.of(
                                0.65f, new BaseItemModel(paths[2]),
                                0.9f, new BaseItemModel(paths[3])
                        ),
                        new BaseItemModel(paths[1])
                ),
                new BaseItemModel(paths[0])
        );
    }
}
