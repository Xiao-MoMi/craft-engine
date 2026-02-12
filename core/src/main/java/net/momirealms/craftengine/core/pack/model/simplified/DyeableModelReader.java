package net.momirealms.craftengine.core.pack.model.simplified;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DyeableModelReader implements SimplifiedModelReader {
    public static final DyeableModelReader INSTANCE = new DyeableModelReader();

    private DyeableModelReader() {}

    @Override
    public Map<String, Object> convert(List<String> textures, List<String> optionalModelPaths, Key id) {
        if (optionalModelPaths.size() >= 2) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_model", "1", String.valueOf(optionalModelPaths.size()));
        }
        boolean autoModelPath = optionalModelPaths.size() != 1;
        Map<String, String> texturesProperty;
        switch (textures.size()) {
            case 1 -> texturesProperty = Map.of("layer0", textures.getFirst());
            case 2 -> texturesProperty = Map.of(
                    "layer0", textures.get(0),
                    "layer1", textures.get(1)
            );
            default -> {
                texturesProperty = new HashMap<>();
                for (int i = 0; i < textures.size(); i++) {
                    texturesProperty.put("layer" + i, textures.get(i));
                }
            }
        }
        return Map.of(
                "type", "model",
                "path", autoModelPath ? id.namespace() + ":item/" + id.value() : optionalModelPaths.getFirst(),
                "generation", Map.of(
                        "parent", "item/generated",
                        "textures", texturesProperty
                ),
                "tints", List.of(
                        Map.of(
                                "type", "minecraft:dye",
                                "default", -6265536
                        )
                )
        );
    }

    @Override
    public @NotNull Map<String, Object> convert(List<String> models) {
        if (models.size() != 1) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_model", "1", String.valueOf(models.size()));
        }
        return Map.of(
                "path", models.getFirst(),
                "tints", List.of(
                        Map.of(
                                "type", "minecraft:dye",
                                "default", -6265536
                        )
                )
        );
    }
}
