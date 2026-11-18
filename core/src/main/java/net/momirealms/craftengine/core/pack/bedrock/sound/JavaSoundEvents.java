package net.momirealms.craftengine.core.pack.bedrock.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

final class JavaSoundEvents {
    static final Event MISSING = new Event("", List.of(), false);
    private static final Event SILENT = new Event("", List.of(), true);
    private final JavaResourcePack resources;
    private final JsonObject mappings;
    private final Map<Key, Key> replacements;
    private final Map<Key, Key> originals = new HashMap<>();
    private final Map<String, JsonObject> vanillaDefinitions = new HashMap<>();
    private final Map<String, JsonObject> customDefinitions = new HashMap<>();
    private final Map<Key, Event> events = new HashMap<>();
    private final Map<Key, Path> audio = new HashMap<>();
    private final Map<Key, Boolean> silentFiles = new HashMap<>();
    private final Set<Key> visiting = new HashSet<>();

    JavaSoundEvents(JavaResourcePack resources, JsonObject mappings, Map<Key, Key> replacements) {
        this.resources = resources;
        this.mappings = mappings;
        this.replacements = replacements;
        replacements.forEach((original, alias) -> this.originals.put(alias, original));
    }

    Event resolve(Key id) throws IOException {
        Event cached = this.events.get(id);
        if (cached != null) {
            return cached;
        }
        if (!this.visiting.add(id)) {
            return MISSING;
        }
        try {
            Event result = this.read(id);
            this.events.put(id, result);
            return result;
        } finally {
            this.visiting.remove(id);
        }
    }

    private Event read(Key id) throws IOException {
        if (id.equals(Key.of("minecraft:intentionally_empty"))) {
            return SILENT;
        }
        Key original = this.originals.getOrDefault(id, id);
        JsonObject custom = this.definition(original, true);
        Key replacement = this.replacements.get(original);
        // CE mutes the carrier event in the Java pack and saves the audible definition under its replacement ID.
        JsonObject restored = replacement == null ? null : this.definition(replacement, true);
        boolean carrierMute = restored != null && this.isCarrierMute(custom);
        if (carrierMute) {
            custom = restored;
            id = original;
        } else if (!id.equals(original)) {
            custom = this.definition(id, true);
        }
        JsonObject vanilla = this.definition(id, false);
        JsonElement mapped = id.isMinecraftNamespace() && !this.resources.filtersVanilla("assets/minecraft/sounds.json")
                ? this.mappings.get(id.value()) : null;
        JsonArray base = sounds(vanilla);
        JsonArray entries = sounds(custom);
        boolean replace = carrierMute || (custom != null && custom.has("replace") && custom.get("replace").getAsBoolean());
        boolean modified = custom != null && (replace || !entries.isEmpty());
        if (modified && !base.isEmpty() && sameSamples(entries, base)) {
            modified = false;
        }
        if (!modified) {
            entries = base;
        } else if (!replace && !base.isEmpty()) {
            JsonArray merged = base.deepCopy();
            merged.addAll(entries);
            entries = merged;
        }
        if (entries.isEmpty()) {
            if (!modified && mapped != null) {
                return new Event(mapped.getAsString(), List.of(), true);
            }
            return custom != null || vanilla != null ? SILENT : MISSING;
        }
        return this.resolveSamples(id, entries, !modified && mapped != null ? mapped.getAsString() : null);
    }

    private Event resolveSamples(Key id, JsonArray entries, @Nullable String nativeSound) throws IOException {
        List<Key> samples = new ArrayList<>();
        Event singleReference = null;
        for (JsonElement entry : entries) {
            Key key = sampleId(entry);
            if (isReference(entry)) {
                Event nested = this.resolve(key);
                if (nested == MISSING) {
                    return MISSING;
                }
                if (entries.size() > 1 && !nested.name().isEmpty() && nested.samples().isEmpty()) {
                    return MISSING;
                }
                if (!nested.vanilla()) {
                    nativeSound = null;
                }
                samples.addAll(nested.samples());
                if (entries.size() == 1) {
                    singleReference = nested;
                }
            } else {
                Path customFile = this.resources.soundResource(key.namespace(), "sounds/" + key.value() + ".ogg", true);
                if (customFile != null) {
                    nativeSound = null;
                    this.audio.put(key, customFile);
                } else if (this.resources.filtersVanilla("assets/" + key.namespace() + "/sounds/" + key.value() + ".ogg")) {
                    return MISSING;
                }
                samples.add(key);
            }
        }
        if (nativeSound != null) {
            return new Event(nativeSound, List.copyOf(samples), true);
        }
        if (singleReference != null) {
            return singleReference;
        }
        if (samples.isEmpty()) {
            return SILENT;
        }
        for (Key sample : samples) {
            if (!this.audio.containsKey(sample)) {
                Path file;
                try {
                    file = this.resources.soundResource(sample.namespace(), "sounds/" + sample.value() + ".ogg", false);
                } catch (IOException unavailable) {
                    file = null;
                }
                this.audio.put(sample, file);
            }
            if (this.audio.get(sample) == null) {
                return MISSING;
            }
        }
        return new Event(id.asString(), List.copyOf(samples), false);
    }

    Path audio(Key id) {
        return this.audio.get(id);
    }

    private JsonObject definition(Key id, boolean custom) throws IOException {
        return this.definitions(id.namespace(), custom).getAsJsonObject(id.value());
    }

    JsonObject definitions(String name, boolean custom) throws IOException {
        Map<String, JsonObject> definitions = custom ? this.customDefinitions : this.vanillaDefinitions;
        JsonObject namespace = definitions.get(name);
        if (namespace == null) {
            try {
                Path file = this.resources.soundResource(name, "sounds.json", custom);
                namespace = file == null ? new JsonObject() : GsonHelper.readJsonObjectFromFile(file);
            } catch (IOException unavailable) {
                if (custom || !name.equals("minecraft")) {
                    throw unavailable;
                }
                // A font-only vanilla cache can still match native sounds while the Mojang asset server is offline.
                namespace = GsonHelper.readJsonFromResource("/internal/sounds.json").getAsJsonObject();
            }
            definitions.put(name, namespace);
        }
        return namespace;
    }

    boolean hasCarrierReplacement(Key id, Key alias) throws IOException {
        return this.definition(alias, true) != null && this.isCarrierMute(this.definition(id, true));
    }

    private boolean isCarrierMute(@Nullable JsonObject definition) throws IOException {
        if (definition == null || !definition.has("replace") || !definition.get("replace").getAsBoolean()) {
            return false;
        }
        JsonArray sounds = sounds(definition);
        if (sounds.isEmpty()) {
            return true;
        }
        if (sounds.size() != 1 || !sounds.get(0).isJsonPrimitive()) {
            return false;
        }
        Key key = Key.of(sounds.get(0).getAsString());
        Boolean cached = this.silentFiles.get(key);
        if (cached != null) {
            return cached;
        }
        Path sound = this.resources.soundResource(key.namespace(), "sounds/" + key.value() + ".ogg", true);
        if (sound == null) {
            return false;
        }
        try (InputStream silence = JavaSoundEvents.class.getResourceAsStream("/silence.ogg")) {
            if (silence == null) {
                return false;
            }
            byte[] bytes = silence.readAllBytes();
            boolean silent = Files.size(sound) == bytes.length && Arrays.equals(bytes, Files.readAllBytes(sound));
            this.silentFiles.put(key, silent);
            return silent;
        }
    }

    static JsonArray sounds(@Nullable JsonObject event) {
        return event == null || !event.has("sounds") ? new JsonArray() : event.getAsJsonArray("sounds");
    }

    private static boolean sameSamples(JsonArray first, JsonArray second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            JsonElement a = first.get(index);
            JsonElement b = second.get(index);
            if (!sampleId(a).equals(sampleId(b)) || isReference(a) != isReference(b)) {
                return false;
            }
        }
        return true;
    }

    static Key sampleId(JsonElement entry) {
        return Key.of(entry.isJsonPrimitive() ? entry.getAsString() : entry.getAsJsonObject().get("name").getAsString());
    }

    static boolean isReference(JsonElement entry) {
        return entry.isJsonObject() && entry.getAsJsonObject().has("type") && entry.getAsJsonObject().get("type").getAsString().equals("event");
    }

    record Event(String name, List<Key> samples, boolean vanilla) {
    }
}
