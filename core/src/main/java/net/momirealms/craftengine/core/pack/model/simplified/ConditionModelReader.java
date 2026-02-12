package net.momirealms.craftengine.core.pack.model.simplified;

import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class ConditionModelReader implements SimplifiedModelReader {
    public static final ConditionModelReader FISHING_ROD = new ConditionModelReader("fishing_rod", "fishing_rod/cast", "_cast");
    public static final ConditionModelReader ELYTRA = new ConditionModelReader("generated", "broken", "_broken");
    public static final ConditionModelReader SHIELD = new ConditionModelReader("", "using_item", "_blocking");
    private final String model;
    private final String property;
    private final String suffix;

    private ConditionModelReader(String model, String property, String suffix) {
        this.model = model;
        this.property = property;
        this.suffix = suffix;
    }

    @Override
    public @Nullable Map<String, Object> convert(List<String> textures, List<String> optionalModelPaths, Key id) {
        if (this.model.isEmpty()) {
            return null;
        }
        if (textures.size() != 2) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_texture", "2", String.valueOf(textures.size()));
        }
        boolean autoModel = optionalModelPaths.isEmpty();
        if (!autoModel && optionalModelPaths.size() != 2) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_model", "2", String.valueOf(optionalModelPaths.size()));
        }
        return Map.of(
                "type", "condition",
                "property", this.property,
                "on-false", Map.of(
                        "path", autoModel ? id.namespace() + ":item/" + id.value() : optionalModelPaths.getFirst(),
                        "generation", Map.of(
                                "parent", "item/" + this.model,
                                "textures", Map.of(
                                        "layer0", textures.getFirst()
                                )
                        )
                ),
                "on-true", Map.of(
                        "path", autoModel ? id.namespace() + ":item/" + id.value() + this.suffix : optionalModelPaths.getLast(),
                        "generation", Map.of(
                                "parent", "item/" + this.model,
                                "textures", Map.of(
                                        "layer0", textures.getLast()
                                )
                        )
                )
        );
    }

    @Override
    public @NotNull Map<String, Object> convert(List<String> models) {
        if (models.size() != 2) {
            throw new LocalizedResourceConfigException("warning.config.item.simplified_model.invalid_model", "2", String.valueOf(models.size()));
        }
        return Map.of(
                "type", "condition",
                "property", this.property,
                "on-false", Map.of(
                        "path", models.getFirst()
                ),
                "on-true", Map.of(
                        "path", models.getLast()
                )
        );
    }
}
