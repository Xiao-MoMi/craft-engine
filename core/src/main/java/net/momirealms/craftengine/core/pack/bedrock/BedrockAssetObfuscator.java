package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.GEOMETRY_PREFIX;

final class BedrockAssetObfuscator {
    private static final String SOUND_DEFINITIONS = "sounds/sound_definitions.json";
    private static final String BLOCK_SOUNDS = "sounds.json";
    private static final List<AnimationIds> ANIMATION_IDS = List.of(
            new AnimationIds("animations", "animation."),
            new AnimationIds("animation_controllers", "controller.animation."),
            new AnimationIds("render_controllers", "controller.render.")
    );

    private BedrockAssetObfuscator() {
    }

    static void obfuscate(Path pack, Path data, Map<String, JsonObject> bundles) throws IOException {
        List<String> textures = new ArrayList<>();
        List<String> sounds = new ArrayList<>();
        List<String> definitions = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(pack)) {
            for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = pack.relativize(file).toString().replace('\\', '/');
                if (name.endsWith(".json")) {
                    definitions.add(name);
                } else if ((name.endsWith(".png") || name.endsWith(".tga")) && (name.startsWith(BedrockPackLayout.CRAFTENGINE_TEXTURES + '/') || name.startsWith(BedrockPackLayout.CRAFTENGINE_PARTICLE_TEXTURES + '/'))) {
                    textures.add(name);
                } else if (name.endsWith(".ogg") && name.startsWith(BedrockPackLayout.CRAFTENGINE_SOUNDS + '/')) {
                    sounds.add(name);
                }
            }
        }

        definitions.addAll(bundles.keySet());
        Map<String, JsonObject> definitionDocuments = new HashMap<>(bundles);
        Map<String, String> identifiers = definitionNames(pack, definitions, definitionDocuments);
        BedrockAttachableSharing.share(pack, definitions, definitionDocuments, identifiers);
        shortenNativeEntities(data.resolve(BedrockPackLayout.NATIVE_MODELS), identifiers);

        Map<String, String> paths = new HashMap<>();
        renameFiles(pack, textures, BedrockPackLayout.CRAFTENGINE_TEXTURES, null, paths);
        renameFiles(pack, sounds, BedrockPackLayout.CRAFTENGINE_SOUNDS, null, paths);
        renameDefinitionFiles(pack, definitions, paths);
        Map<String, String> values = new HashMap<>(identifiers);
        values.putAll(paths);

        BedrockAtlasObfuscator.obfuscate(pack, data);
        Map<String, String> clientIds = BedrockIdentityObfuscator.rewritePlans(data);
        Map<String, String> clientKeys = new HashMap<>(identifiers);
        clientKeys.putAll(clientIds);
        UnaryOperator<String> clientValues = value -> {
            String replacement = values.get(value);
            if (replacement != null) {
                return replacement;
            }
            replacement = clientIds.get(value);
            return replacement != null ? replacement : BedrockIdentityObfuscator.rewriteMolang(value, clientIds);
        };
        for (String definition : definitions) {
            Path file = pack.resolve(paths.getOrDefault(definition, definition));
            JsonObject document = definitionDocuments.remove(definition);
            if (bundles.containsKey(definition)) {
                GsonHelper.replaceStrings(document, clientValues, clientKeys);
                Files.createDirectories(file.getParent());
                GsonHelper.writeJsonFile(document, file);
            } else if (document == null) {
                rewriteReferences(file, clientValues, clientKeys);
            } else {
                rewriteReferences(file, document, clientValues, clientKeys);
            }
        }
        for (String path : List.of(RegistrationPlanKind.BLOCK.file(), BedrockPackLayout.SCOREBOARD)) {
            Path definition = data.resolve(path);
            if (Files.exists(definition)) {
                rewriteReferences(definition, values::get, Map.of());
            }
        }
        Path soundMappings = data.resolve(BedrockPackLayout.SOUND_MAPPINGS);
        if (Files.exists(soundMappings)) {
            JsonObject mappings = GsonHelper.readJsonFromFile(soundMappings).getAsJsonObject();
            if (GsonHelper.replaceStringValues(mappings, "events", identifiers)) {
                GsonHelper.writeJsonFile(mappings, soundMappings);
            }
        }
    }

    private static Map<String, String> definitionNames(Path pack, List<String> definitions, Map<String, JsonObject> documents) throws IOException {
        Map<String, String> names = new HashMap<>();
        Set<String> geometries = new HashSet<>();
        for (String file : definitions) {
            if (file.startsWith("models/") && file.endsWith(".geo.json")) {
                collectGeometryIds(readDefinition(pack, file, documents), geometries);
            }
        }
        names.putAll(shortNames(geometries, GEOMETRY_PREFIX, GEOMETRY_PREFIX, "geometry.item_display.", "geometry.ce_native_"));
        for (AnimationIds type : ANIMATION_IDS) {
            Set<String> identifiers = new HashSet<>();
            for (String file : definitions) {
                if (file.startsWith(type.section() + '/')) {
                    identifiers.addAll(definitionKeys(pack, file, type.section(), documents));
                }
            }
            String prefix = type.prefix();
            names.putAll(shortNames(identifiers, prefix + BedrockPackLayout.NAMESPACE + '.', prefix + BedrockPackLayout.NAMESPACE + '.', prefix + "item_display.", prefix + "ce_native_"));
        }
        names.putAll(shortNames(definitionKeys(pack, SOUND_DEFINITIONS, "sound_definitions", documents), "ce.sound.", "ce.sound."));
        names.putAll(shortNames(definitionKeys(pack, BLOCK_SOUNDS, "block_sounds", documents), "ce:block_", "ce:block_"));
        return names;
    }

    private static Set<String> definitionKeys(Path pack, String file, String section, Map<String, JsonObject> documents) throws IOException {
        if (!documents.containsKey(file) && !Files.exists(pack.resolve(file))) {
            return Set.of();
        }
        JsonObject definitions = readDefinition(pack, file, documents).getAsJsonObject(section);
        return definitions == null ? Set.of() : definitions.keySet();
    }

    private static JsonObject readDefinition(Path pack, String file, Map<String, JsonObject> documents) throws IOException {
        JsonObject document = documents.get(file);
        if (document == null) {
            document = GsonHelper.readJsonFromFile(pack.resolve(file)).getAsJsonObject();
            documents.put(file, document);
        }
        return document;
    }

    private static void shortenNativeEntities(Path file, Map<String, String> identifiers) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        JsonObject models = GsonHelper.readJsonFromFile(file).getAsJsonObject();
        Set<String> ids = new HashSet<>();
        for (JsonElement model : models.asMap().values()) {
            ids.add(model.getAsJsonObject().get("identifier").getAsString());
        }
        Map<String, String> names = shortNames(ids, "ce:native_", "ce:native_");
        if (names.isEmpty()) {
            return;
        }
        for (JsonElement model : models.asMap().values()) {
            GsonHelper.replaceStringValues(model.getAsJsonObject(), "identifier", names);
        }
        GsonHelper.writeJsonFile(models, file);
        identifiers.putAll(names);
    }

    private static void renameDefinitionFiles(Path pack, List<String> definitions, Map<String, String> paths) throws IOException {
        List<String> attachables = definitions.stream()
                .filter(file -> file.startsWith("attachables/ce/") && file.endsWith(".attachable.json")).toList();
        List<String> entities = definitions.stream()
                .filter(file -> (file.startsWith("entity/ce/native/") && file.endsWith(".entity.json")) || file.equals("entity/item_display.entity.json") || file.equals("entity/item_display_held.entity.json")).toList();
        List<String> ui = definitions.stream().filter(file -> file.startsWith("ui/ce/")).toList();
        renameFiles(pack, attachables, "attachables/ce", ".attachable.json", paths);
        renameFiles(pack, entities, "entity/ce", ".entity.json", paths);
        renameFiles(pack, ui, "ui/ce", ".json", paths);
    }

    private static void collectGeometryIds(JsonObject document, Set<String> ids) {
        JsonArray geometries = document.getAsJsonArray("minecraft:geometry");
        if (geometries != null) {
            for (JsonElement geometry : geometries) {
                ids.add(geometry.getAsJsonObject().getAsJsonObject("description").get("identifier").getAsString());
            }
        }
    }

    static Map<String, String> shortNames(Collection<String> identifiers, String prefix, String... ownedPrefixes) {
        Map<String, String> aliases = new HashMap<>();
        Set<String> sorted = new TreeSet<>(identifiers);
        int index = 0;
        for (String identifier : sorted) {
            if (!startsWithAny(identifier, ownedPrefixes)) {
                continue;
            }
            String name;
            do {
                name = prefix + Integer.toString(index++, 36);
            } while (sorted.contains(name));
            aliases.put(identifier, name);
        }
        return aliases;
    }

    private static boolean startsWithAny(String name, String[] prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void renameFiles(Path pack, List<String> files, String directory, @Nullable String suffix, Map<String, String> aliases) throws IOException {
        if (files.isEmpty()) {
            return;
        }
        Files.createDirectories(pack.resolve(directory));
        Set<String> occupied = new HashSet<>();
        for (String file : files) {
            occupied.add(file.substring(0, extensionStart(file, suffix)));
        }
        int index = 0;
        for (String file : files) {
            int dot = extensionStart(file, suffix);
            String stem = file.substring(0, dot);
            String extension = file.substring(dot);
            String name = aliases.get(stem);
            if (name == null) {
                do {
                    name = directory + '/' + Integer.toString(index++, 36);
                } while (occupied.contains(name) || Files.exists(pack.resolve(name + extension)));
                aliases.put(stem, name);
            }
            String target = name + extension;
            Files.move(pack.resolve(file), pack.resolve(target));
            aliases.put(file, target);
        }
    }

    private static int extensionStart(String file, @Nullable String suffix) {
        return suffix == null ? file.lastIndexOf('.') : file.length() - suffix.length();
    }

    private static void rewriteReferences(Path file, UnaryOperator<String> values, Map<String, String> keys) throws IOException {
        rewriteReferences(file, GsonHelper.readJsonFromFile(file), values, keys);
    }

    private static void rewriteReferences(Path file, JsonElement document, UnaryOperator<String> values, Map<String, String> keys) throws IOException {
        if (GsonHelper.replaceStrings(document, values, keys)) {
            GsonHelper.writeJsonFile(document, file);
        }
    }

    private record AnimationIds(String section, String prefix) {
    }
}
