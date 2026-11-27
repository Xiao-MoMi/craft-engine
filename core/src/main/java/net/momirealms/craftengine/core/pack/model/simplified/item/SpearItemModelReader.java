package net.momirealms.craftengine.core.pack.model.simplified.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.pack.model.bbmodel.BBModelConverter;
import net.momirealms.craftengine.core.pack.model.definition.BaseItemModel;
import net.momirealms.craftengine.core.pack.model.definition.ItemModel;
import net.momirealms.craftengine.core.pack.model.definition.SelectItemModel;
import net.momirealms.craftengine.core.pack.model.definition.select.DisplayContextSelectProperty;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.generation.ModelGenerationHolder;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class SpearItemModelReader implements SimplifiedItemModelReader {
    public static final SpearItemModelReader INSTANCE = new SpearItemModelReader();
    private static final Key GENERATED = Key.of("item/generated");
    private static final Key SPEAR_IN_HAND = Key.of("item/spear_in_hand");
    private static final List<JsonElement> CASES = List.of(
            new JsonPrimitive("gui"),
            new JsonPrimitive("ground"),
            new JsonPrimitive("fixed"),
            new JsonPrimitive("on_shelf")
    );

    private SpearItemModelReader() {}

    @Override
    @NotNull
    public ItemModel read(ConfigValue textureValue, Optional<ConfigValue> optionalModelValue, Key id, Consumer<ModelGenerationHolder> generations) {
        List<Key> textures = textureValue.getAsFixedSizeList(2, ConfigValue::getAsAssetPath);
        List<Key> models = optionalModelValue.map(it -> it.getAsFixedSizeList(2, ConfigValue::getAsAssetPath)).orElse(null);
        return build(
                models != null ? models.get(0) : Key.of(id.namespace(), "item/" + id.value()),
                ModelGeneration.builder()
                        .parentModelPath(GENERATED)
                        .texturesOverride(Map.of("layer0", textures.get(0).asMinimalString()))
                        .build(),
                models != null ? models.get(1) : Key.of(id.namespace(), "item/" + id.value() + "_in_hand"),
                ModelGeneration.builder()
                        .parentModelPath(SPEAR_IN_HAND)
                        .texturesOverride(Map.of("layer0", textures.get(1).asMinimalString()))
                        .build(),
                generations
        );
    }

    @Override
    public ItemModel read(ConfigValue modelValue) {
        List<Key> models = modelValue.getAsFixedSizeList(2, ConfigValue::getAsAssetPath);
        return new SelectItemModel(
                DisplayContextSelectProperty.INSTANCE,
                Map.of(Either.right(CASES), new BaseItemModel(models.get(0))),
                new BaseItemModel(models.get(1))
        );
    }

    @Override
    public int modelCount() {
        return 2;
    }

    @Override
    public ItemModel buildFromBlueprints(List<BBModelConverter.Converted> blueprints, Consumer<ModelGenerationHolder> generations) {
        BBModelConverter.Converted gui = blueprints.get(0);
        BBModelConverter.Converted inHand = blueprints.get(1);
        return build(
                gui.model(), ModelGeneration.raw(gui.json(), gui.textures(), gui.textureMetadata()),
                inHand.model(), ModelGeneration.raw(inHand.json(), inHand.textures(), inHand.textureMetadata()), generations
        );
    }

    private ItemModel build(Key guiPath, ModelGeneration guiGeneration, Key inHandPath, ModelGeneration inHandGeneration, Consumer<ModelGenerationHolder> generations) {
        generations.accept(new ModelGenerationHolder(guiPath, guiGeneration));
        generations.accept(new ModelGenerationHolder(inHandPath, inHandGeneration));
        return new SelectItemModel(
                DisplayContextSelectProperty.INSTANCE,
                Map.of(Either.right(CASES), new BaseItemModel(guiPath)),
                new BaseItemModel(inHandPath)
        );
    }
}
