package net.momirealms.craftengine.core.pack.model.simplified;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public final class BowModelReader implements SimplifiedModelReader {
    public static final BowModelReader INSTANCE = new BowModelReader();

    private BowModelReader() {}

    @Override
    public @NotNull Map<String, Object> convert(List<String> textures, List<String> optionalModelPaths, Key id) {
        if (textures.size() != 4) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_texture", "4", String.valueOf(textures.size()));
        }
        boolean autoModel = optionalModelPaths.isEmpty();
        if (!autoModel && optionalModelPaths.size() != 4) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_model", "4", String.valueOf(optionalModelPaths.size()));
        }
        return Map.of(
                "type", "condition",
                "property", "using_item",
                "on-false", Map.of(
                        "path", autoModel ? id.namespace() + ":item/" + id.value() : optionalModelPaths.get(0),
                        "generation", Map.of(
                                "parent", "item/bow",
                                "textures", Map.of(
                                        "layer0", textures.get(0)
                                )
                        )
                ),
                "on-true", Map.of(
                        "type", "range_dispatch",
                        "property", "use_duration",
                        "scale", 0.05,
                        "entries", List.of(
                                Map.of(
                                        "model", Map.of(
                                                "path", autoModel ? id.namespace() + ":item/" + id.value() + "_pulling_1" : optionalModelPaths.get(2),
                                                "generation", Map.of(
                                                        "parent", "item/bow_pulling_1",
                                                        "textures", Map.of(
                                                                "layer0", textures.get(2)
                                                        )
                                                )
                                        ),
                                        "threshold", 0.65
                                ),
                                Map.of(
                                        "model", Map.of(
                                                "path", autoModel ? id.namespace() + ":item/" + id.value() + "_pulling_2" : optionalModelPaths.get(3),
                                                "generation", Map.of(
                                                        "parent", "item/bow_pulling_2",
                                                        "textures", Map.of(
                                                                "layer0", textures.get(3)
                                                        )
                                                )
                                        ),
                                        "threshold", 0.9
                                )
                        ),
                        "fallback", Map.of(
                                "path", autoModel ? id.namespace() + ":item/" + id.value() + "_pulling_0" : optionalModelPaths.get(1),
                                "generation", Map.of(
                                        "parent", "item/bow_pulling_0",
                                        "textures", Map.of(
                                                "layer0", textures.get(1)
                                        )
                                )
                        )
                )
        );
    }

    @Override
    public @NotNull Map<String, Object> convert(List<String> models) {
        if (models.size() != 4) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_model", "4", String.valueOf(models.size()));
        }
        return Map.of(
                "type", "condition",
                "property", "using_item",
                "on-false", Map.of(
                        "path", models.get(0)
                ),
                "on-true", Map.of(
                        "type", "range_dispatch",
                        "property", "use_duration",
                        "scale", 0.05,
                        "entries", List.of(
                                Map.of(
                                        "model", Map.of(
                                                "path", models.get(2)
                                        ),
                                        "threshold", 0.65
                                ),
                                Map.of(
                                        "model", Map.of(
                                                "path", models.get(3)
                                        ),
                                        "threshold", 0.9
                                )
                        ),
                        "fallback", Map.of(
                                "path", models.get(1)
                        )
                )
        );
    }
}
