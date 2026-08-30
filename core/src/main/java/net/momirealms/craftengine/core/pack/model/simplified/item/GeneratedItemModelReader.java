package net.momirealms.craftengine.core.pack.model.simplified.item;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.pack.model.bbmodel.BBModelConverter;
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
import net.momirealms.craftengine.core.util.Map2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GeneratedItemModelReader implements SimplifiedItemModelReader {
    public static final GeneratedItemModelReader GENERATED = new GeneratedItemModelReader(Key.of("item/generated"), List.of());
    public static final GeneratedItemModelReader HANDHELD = new GeneratedItemModelReader(Key.of("item/handheld"), List.of());
    public static final GeneratedItemModelReader HANDHELD_MACE = new GeneratedItemModelReader(Key.of("item/handheld_mace"), List.of());
    public static final GeneratedItemModelReader HANDHELD_ROD = new GeneratedItemModelReader(Key.of("item/handheld_rod"), List.of());
    public static final GeneratedItemModelReader LEATHER = new GeneratedItemModelReader(Key.of("item/generated"), List.of(new SimpleDefaultTint(Key.of("dye"), Either.left(16777215))));
    public static final GeneratedItemModelReader FIREWORK_STAR = new GeneratedItemModelReader(Key.of("item/generated"), List.of(new ConstantTint(Either.left(-1)), new SimpleDefaultTint(Key.of("firework"), Either.left(16777215))));

    private final Key parentModel;
    private final List<Tint> tints;

    private GeneratedItemModelReader(Key model, List<Tint> tints) {
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
            case 2 -> texturesProperty = Map2.of(
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

    @Override
    public int modelCount() {
        return 1;
    }

    @Override
    public ItemModel buildFromBlueprints(List<BBModelConverter.Converted> blueprints) {
        BBModelConverter.Converted converted = blueprints.getFirst();
        return new BaseItemModel(converted.model(), this.tints, ModelGeneration.raw(converted.json(), converted.textures()));
    }
}
