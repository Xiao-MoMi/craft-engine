package net.momirealms.craftengine.core.pack.model.simplified;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public final class CrossbowModelReader implements SimplifiedModelReader {
    public static final CrossbowModelReader INSTANCE = new CrossbowModelReader();

    private CrossbowModelReader() {}

    @Override
    public @NotNull Map<String, Object> convert(List<String> textures, List<String> optionalModelPaths, Key id) {
        if (textures.size() != 6) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_texture", "6", String.valueOf(textures.size()));
        }
        boolean autoModel = optionalModelPaths.isEmpty();
        if (!autoModel && optionalModelPaths.size() != 6) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_model", "6", String.valueOf(optionalModelPaths.size()));
        }
        return Map.of(
                "type", "condition",
                "property", "using_item",
                "on-false", Map.of(
                        "type", "select",
                        "property", "charge_type",
                        "cases", List.of(
                                Map.of(
                                        "when", "arrow",
                                        "model", Map.of(
                                                "type", "model",
                                                "path", autoModel ? id.namespace() + ":item/" + id.value() + "_arrow" : optionalModelPaths.get(4),
                                                "generation", Map.of(
                                                        "parent", "item/crossbow_arrow",
                                                        "textures", Map.of(
                                                                "layer0", textures.get(4)
                                                        )
                                                )
                                        )
                                ),
                                Map.of(
                                        "when", "rocket",
                                        "model", Map.of(
                                                "type", "model",
                                                "path", autoModel ? id.namespace() + ":item/" + id.value() + "_firework" : optionalModelPaths.get(5),
                                                "generation", Map.of(
                                                        "parent", "item/crossbow_firework",
                                                        "textures", Map.of(
                                                                "layer0", textures.get(5)
                                                        )
                                                )
                                        )
                                )
                        ),
                        "fallback", Map.of(
                                "type", "model",
                                "path", autoModel ? id.namespace() + ":item/" + id.value() : optionalModelPaths.get(0),
                                "generation", Map.of(
                                        "parent", "item/crossbow",
                                        "textures", Map.of(
                                                "layer0", textures.get(0)
                                        )
                                )
                        )
                ),
                "on-true", Map.of(
                        "type", "range_dispatch",
                        "property", "crossbow/pull",
                        "entries", List.of(
                                Map.of(
                                        "model", Map.of(
                                                "type", "model",
                                                "path", autoModel ? id.namespace() + ":item/" + id.value() + "_pulling_1" : optionalModelPaths.get(2),
                                                "generation", Map.of(
                                                        "parent", "item/crossbow_pulling_1",
                                                        "textures", Map.of(
                                                                "layer0", textures.get(2)
                                                        )
                                                )
                                        ),
                                        "threshold", 0.58
                                ),
                                Map.of(
                                        "model", Map.of(
                                                "type", "model",
                                                "path", autoModel ? id.namespace() + ":item/" + id.value() + "_pulling_2" : optionalModelPaths.get(3),
                                                "generation", Map.of(
                                                        "parent", "item/crossbow_pulling_2",
                                                        "textures", Map.of(
                                                                "layer0", textures.get(3)
                                                        )
                                                )
                                        ),
                                        "threshold", 1.0
                                )
                        ),
                        "fallback", Map.of(
                                "type", "model",
                                "path", autoModel ? id.namespace() + ":item/" + id.value() + "_pulling_0" : optionalModelPaths.get(1),
                                "generation", Map.of(
                                        "parent", "item/crossbow_pulling_0",
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
        if (models.size() != 6) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_model", "6", String.valueOf(models.size()));
        }
        return Map.of(
                "type", "condition",
                "property", "using_item",
                "on-false", Map.of(
                        "type", "select",
                        "property", "charge_type",
                        "cases", List.of(
                                Map.of(
                                        "when", "arrow",
                                        "model", Map.of(
                                                "type", "model",
                                                "path", models.get(4)
                                        )
                                ),
                                Map.of(
                                        "when", "rocket",
                                        "model", Map.of(
                                                "type", "model",
                                                "path", models.get(5)
                                        )
                                )
                        ),
                        "fallback", Map.of(
                                "type", "model",
                                "path", models.get(0)
                        )
                ),
                "on-true", Map.of(
                        "type", "range_dispatch",
                        "property", "crossbow/pull",
                        "entries", List.of(
                                Map.of(
                                        "model", Map.of(
                                                "type", "model",
                                                "path", models.get(2)
                                        ),
                                        "threshold", 0.58
                                ),
                                Map.of(
                                        "model", Map.of(
                                                "type", "model",
                                                "path", models.get(3)
                                        ),
                                        "threshold", 1.0
                                )
                        ),
                        "fallback", Map.of(
                                "type", "model",
                                "path", models.get(1)
                        )
                )
        );
    }
}
