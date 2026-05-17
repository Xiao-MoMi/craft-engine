package net.momirealms.craftengine.core.pack.model.simplified;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.CompositeItemModel;
import net.momirealms.craftengine.core.pack.model.definition.EmptyItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.tint.ConstantTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.SimpleDefaultTint;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GeneratedModelReader implements SimplifiedModelReader {
    public static final GeneratedModelReader GENERATED = new GeneratedModelReader(Key.of("item/generated"), List.of());
    public static final GeneratedModelReader HANDHELD = new GeneratedModelReader(Key.of("item/handheld"), List.of());
    public static final GeneratedModelReader LEATHER = new GeneratedModelReader(Key.of("item/generated"), List.of(new SimpleDefaultTint(Key.of("dye"), Either.left(16777215))));
    public static final GeneratedModelReader FIREWORK_STAR = new GeneratedModelReader(Key.of("item/generated"), List.of(new ConstantTint(Either.left(-1)), new SimpleDefaultTint(Key.of("firework"), Either.left(16777215))));

    private final Key parentModel;
    private final List<Tint> tints;

    private GeneratedModelReader(Key model, List<Tint> tints) {
        this.parentModel = model;
        this.tints = tints;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public ItemModel read(ConfigValue textureValue, Optional<ConfigValue> optionalModelValue, Key id) {
        List<Key> textures = textureValue.getAsList(ConfigValue::getAsAssetPath);
        if (textures.isEmpty()) {
            return EmptyItemModel.INSTANCE;
        }
        List<Key> models = optionalModelValue.map(it -> it.getAsFixedSizeList(1, ConfigValue::getAsAssetPath)).orElse(null);
        boolean autoModel = models == null;
        Map<String, String> texturesProperty;
        switch (textures.size()) {
            case 1 -> texturesProperty = Map.of("layer0", textures.getFirst().asMinimalString());
            case 2 -> texturesProperty = Map.of(
                    "layer0", textures.get(0).asMinimalString(),
                    "layer1", textures.get(1).asMinimalString()
            );
            default -> {
                texturesProperty = new HashMap<>();
                for (int i = 0; i < textures.size(); i++) {
                    texturesProperty.put("layer" + i, textures.get(i).asMinimalString());
                }
            }
        }
        return new BaseItemModel(
                autoModel ? Key.of(id.namespace(), "item/" + id.value()) : models.getFirst(),
                this.tints,
                ModelGeneration.builder()
                        .parentModelPath(this.parentModel)
                        .texturesOverride(texturesProperty)
                        .build()
        );
    }

    @Override
    public ItemModel read(ConfigValue modelValue) {
        List<Key> models = modelValue.getAsList(ConfigValue::getAsAssetPath);
        if (models.isEmpty()) {
            return EmptyItemModel.INSTANCE;
        } else if (models.size() == 1) {
            return new BaseItemModel(models.getFirst(), this.tints);
        } else {
            return new CompositeItemModel(models.stream().map(it -> (ItemModel) new BaseItemModel(it, this.tints)).toList());
        }
    }
}
