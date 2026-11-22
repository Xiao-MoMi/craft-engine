package net.momirealms.craftengine.core.pack.bedrock;

import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record BedrockEntitySettings(Map<String, BedrockModelIntegration> integrations,
                                    BedrockEntityDye dye,
                                    Map<Key, Model> overrides,
                                   int boneVisibilityIntervalTicks) {
    public static final BedrockEntitySettings DEFAULT = new BedrockEntitySettings(Map.of(), BedrockEntityDye.DEFAULT, Map.of(), 2);

    public BedrockEntitySettings {
        integrations = Map.copyOf(integrations);
        overrides = Map.copyOf(overrides);
    }

    public static BedrockEntitySettings read(ConfigSection entity) {
        Map<String, BedrockModelIntegration> integrations = new HashMap<>();
        ConfigSection modes = entity.getSection("integrations");
        if (modes != null) {
            for (String plugin : List.of("bettermodel", "modelengine")) {
                integrations.put(plugin, modes.getEnum(plugin, BedrockModelIntegration.class, BedrockModelIntegration.DISABLED, BedrockModelIntegration::byId));
            }
        }
        Map<Key, Model> overrides = new HashMap<>();
        for (ConfigSection entry : entity.getSectionList("overrides", section -> section)) {
            List<Key> models = entry.getNonNullValue("model", ConfigConstants.ARGUMENT_IDENTIFIER, value -> value.getAsNonEmptyList(ConfigValue::getAsIdentifier));
            BedrockModelIntegration mode = entry.getEnum("mode", BedrockModelIntegration.class, null, BedrockModelIntegration::byId);
            BedrockEntityDye dye = BedrockEntityDye.read(entry, List.of());
            for (Key id : models) {
                BedrockModelIntegration resolvedMode = mode == null ? integrations.getOrDefault(id.namespace(), BedrockModelIntegration.DISABLED) : mode;
                if (overrides.putIfAbsent(id, new Model(resolvedMode, dye)) != null) {
                    throw new IllegalArgumentException("Duplicate Bedrock model override: " + id);
                }
            }
        }
        int intervalMs = entity.getInt("bone-visibility-check-interval", 100);
        int intervalTicks = 1 + (Math.max(1, intervalMs) - 1) / 50;
        return new BedrockEntitySettings(integrations, BedrockEntityDye.read(entity, List.of("#FF8080")), overrides, intervalTicks);
    }

    public BedrockModelIntegration mode(Key model) {
        Model override = this.overrides.get(model);
        return override == null ? this.integrations.getOrDefault(model.namespace(), BedrockModelIntegration.DISABLED) : override.mode();
    }

    public BedrockEntityDye dye(Key model) {
        Model override = this.overrides.get(model);
        return override == null ? this.dye : override.dye();
    }

    public boolean uses(String plugin, BedrockModelIntegration mode) {
        return this.integrations.get(plugin) == mode || this.overrides.entrySet().stream()
                .anyMatch(entry -> entry.getKey().namespace().equals(plugin) && entry.getValue().mode() == mode);
    }

    public record Model(BedrockModelIntegration mode, BedrockEntityDye dye) {
    }
}
