package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRegistrationResult.Diagnostic;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRegistrationResult.Registration;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.CustomItemIcons;
import org.geysermc.geyser.api.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class GeyserItemRegistrationAdapter {

    public GeyserItemRegistrationResult readPack(Path path) throws IOException {
        try (ZipFile archive = new ZipFile(path.toFile())) {
            ZipEntry entry = archive.getEntry(BedrockPackLayout.ITEM_REGISTRATION_PLAN);
            if (entry == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8)) {
                return convert(JsonParser.parseReader(reader).getAsJsonObject());
            }
        }
    }

    public GeyserItemRegistrationResult convert(JsonObject plan) {
        if (!text(plan, "kind").equals("craftengine:item_registration_plan") || plan.get("format_version").getAsInt() != 1) {
            throw new IllegalArgumentException("Unsupported registration plan format");
        }
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (JsonElement value : plan.getAsJsonArray("diagnostics")) {
            JsonObject diagnostic = value.getAsJsonObject();
            diagnostics.add(new Diagnostic(text(diagnostic, "severity"), "", text(diagnostic, "model"), text(diagnostic, "definition_file"), text(diagnostic, "code") + ": " + diagnostic.get("details")));
        }
        if (plan.get("has_errors").getAsBoolean() || hasErrors(diagnostics)) {
            if (!hasErrors(diagnostics)) {
                diagnostics.add(new Diagnostic("ERROR", "", "", "", "The source plan is marked as having errors"));
            }
            return new GeyserItemRegistrationResult(List.of(), diagnostics);
        }
        int sourceVersion = version(text(plan, "source_version"));
        List<Registration> registrations = new ArrayList<>();
        Set<String> identifiers = new HashSet<>();
        for (JsonElement value : plan.getAsJsonArray("entries")) {
            JsonObject entry = value.getAsJsonObject();
            List<String> warnings = new ArrayList<>();
            try {
                if (!identifiers.add(text(entry, "bedrock_identifier"))) {
                    throw new IllegalArgumentException("Duplicate Bedrock item identifier");
                }
                registrations.add(convertEntry(entry, sourceVersion, warnings));
            } catch (RuntimeException exception) {
                diagnostics.add(diagnostic(entry, "ERROR", exception.getMessage()));
            }
            for (String warning : warnings) {
                diagnostics.add(diagnostic(entry, "WARNING", warning));
            }
        }
        return new GeyserItemRegistrationResult(hasErrors(diagnostics) ? List.of() : registrations, diagnostics);
    }

    private Registration convertEntry(JsonObject entry, int sourceVersion, List<String> warnings) {
        if (sourceVersion < 12104) {
            throw new IllegalArgumentException("Legacy model mapping is not implemented");
        }
        Identifier javaItem = Identifier.of(text(entry, "java_item"));
        Identifier bedrockId = Identifier.of(text(entry, "bedrock_identifier"));
        Identifier model = Identifier.of(text(entry, "model"));
        if (bedrockId.vanilla()) {
            throw new IllegalArgumentException("Bedrock custom item identifiers cannot use the minecraft namespace");
        }
        if (!javaItem.vanilla()) {
            throw new IllegalArgumentException("Non-vanilla Java item registration is not implemented");
        }
        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockId, model);
        builder.bedrockOptions(CustomItemBedrockOptions.builder().icons(CustomItemIcons.builder()
                .defaultIcon(entry.getAsJsonObject("resources").get("icon").getAsString()).build()));
        JsonObject snapshot = entry.getAsJsonObject("component_snapshot");
        JsonObject overrides = entry.getAsJsonObject("component_overrides");
        for (JsonElement removed : entry.getAsJsonArray("removed_components")) {
            if (overrides.has(removed.getAsString())) {
                throw new IllegalArgumentException("A component cannot be both overridden and removed: " + removed.getAsString());
            }
        }
        Set<String> targets = new HashSet<>();
        // 使用支持组件的有效快照，而不只使用覆写，避免继承不同 Java 版本的目标默认值。
        for (Map.Entry<String, JsonElement> component : new TreeMap<>(snapshot.asMap()).entrySet()) {
            String key = component.getKey();
            JsonObject state = component.getValue().getAsJsonObject();
            String source = text(state, "source");
            boolean present = switch (source) {
                case "DEFAULT", "OVERRIDE" -> true;
                case "REMOVED", "ABSENT" -> false;
                default -> throw new IllegalArgumentException("Unknown component source " + source);
            };
            if (!GeyserItemComponentConverter.supports(key)) {
                if (key.equals("minecraft:interact_animation")) {
                    if (source.equals("OVERRIDE") || source.equals("REMOVED")) {
                        warnings.add(key + " is retained as a resource requirement; the API has no interact animation component");
                    }
                    continue;
                }
                if (source.equals("OVERRIDE") || source.equals("REMOVED")) {
                    throw new IllegalArgumentException("Unsupported component " + key);
                }
                continue;
            }
            String target = GeyserItemComponentConverter.targetKey(key);
            if (!targets.add(target)) {
                throw new IllegalArgumentException("Components map to the same API component: " + target);
            }
            try {
                if (present) {
                    GeyserItemComponentConverter.apply(builder, key, state.get("value"), sourceVersion, warnings);
                } else {
                    builder.removeComponent(Identifier.of(target));
                }
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException(key + ": " + exception.getMessage(), exception);
            }
        }
        this.validateStack(snapshot);
        return new Registration(javaItem, builder.build());
    }

    private void validateStack(JsonObject snapshot) {
        int stackSize = this.componentIntValue(snapshot, "minecraft:max_stack_size", 1);
        int maxDamage = this.componentIntValue(snapshot, "minecraft:max_damage", 0);
        if (stackSize < 1 || stackSize > 99 || maxDamage < 0 || (stackSize > 1 && maxDamage > 0)) {
            throw new IllegalArgumentException("Invalid max stack size/damage combination");
        }
        JsonObject equippable = snapshot.getAsJsonObject("minecraft:equippable");
        if (stackSize > 1 && equippable != null && equippable.has("value") && !equippable.get("value").isJsonNull()) {
            throw new IllegalArgumentException("Equippable Bedrock items must have max stack size 1");
        }
    }

    private int componentIntValue(JsonObject snapshot, String key, int fallback) {
        JsonObject component = snapshot.getAsJsonObject(key);
        if (component == null) {
            return fallback;
        }
        return switch (text(component, "source")) {
            case "DEFAULT", "OVERRIDE" -> component.get("value").getAsInt();
            case "ABSENT", "REMOVED" -> fallback;
            default -> throw new IllegalArgumentException("Unknown component source " + text(component, "source"));
        };
    }

    private int version(String value) {
        String[] parts = value.split("\\.");
        return Integer.parseInt(parts[0]) * 10000 + Integer.parseInt(parts[1]) * 100
                + (parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
    }

    private boolean hasErrors(List<Diagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity().equals("ERROR"));
    }

    private Diagnostic diagnostic(JsonObject entry, String severity, String message) {
        return new Diagnostic(severity, text(entry, "bedrock_identifier"), text(entry, "model"),
                text(entry, "definition_file"), message);
    }

    private String text(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }
}
