package net.momirealms.craftengine.core.pack.bedrock.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

final class PlaybackSounds {
    private final JavaResourcePack pack;
    private final JavaSoundEvents source;
    private final Set<Key> customFiles;
    private final Map<Key, Key> restored = new HashMap<>();
    private final Map<Key, Event> cache = new HashMap<>();
    private final Set<Key> visiting = new HashSet<>();
    private final Map<Key, Path> audio = new HashMap<>();
    private static final Event MISSING = new Event(false, true, List.of());

    PlaybackSounds(JavaResourcePack pack, JavaSoundEvents source, Map<Key, Key> replacements) throws IOException {
        this.pack = pack;
        this.source = source;
        this.customFiles = pack.customSoundFiles();
        for (Map.Entry<Key, Key> entry : replacements.entrySet()) {
            if (source.hasCarrierReplacement(entry.getKey(), entry.getValue())) {
                this.restored.put(entry.getKey(), entry.getValue());
            }
        }
    }

    void write(Path root, JsonObject mappings, JsonObject definitions, Map<Key, String> copied) throws IOException {
        Set<Key> candidates = new TreeSet<>(Comparator.comparing(Key::asString));
        for (String namespace : this.pack.soundNamespaces()) {
            for (String name : this.source.definitions(namespace, true).keySet()) {
                candidates.add(Key.of(namespace, name));
            }
        }
        if (candidates.isEmpty() && this.customFiles.isEmpty()) {
            return;
        }
        // A replaced OGG or nested event can also change vanilla events absent from the custom sounds.json.
        for (String name : this.source.definitions("minecraft", false).keySet()) {
            candidates.add(Key.of("minecraft", name));
        }
        candidates.removeAll(this.restored.values());
        JsonObject aliases = new JsonObject();
        this.restored.forEach((original, alias) -> aliases.addProperty(alias.asMinimalString(), original.asMinimalString()));
        JsonObject overrides = new JsonObject();
        for (Key id : candidates) {
            Event event = this.resolve(id);
            if (!event.changed()) {
                continue;
            }
            if (event.missing() || !this.locateAudio(event)) {
                TranslationManager.warn("resource_pack.bedrock.sound.unresolved", id.asString());
                continue;
            }
            String target = "craftengine.sound." + HashUtils.sha1(id.asString().getBytes(StandardCharsets.UTF_8));
            JsonArray files = new JsonArray();
            for (Sample sample : event.samples()) {
                String path = BedrockSounds.copyAudio(root, sample.id(), this.audio.get(sample.id()), copied);
                files.add(sample.encode(path));
            }
            JsonObject definition = GsonHelper.createObject("sounds", files);
            JsonObject block = definitions.getAsJsonObject(id.asString());
            if (block != null && files.equals(block.getAsJsonArray("sounds"))) {
                target = id.asString();
            } else {
                definition.addProperty("category", "neutral");
                definitions.add(target, definition);
            }
            overrides.addProperty(id.asMinimalString(), target);
        }
        if (!overrides.isEmpty()) {
            mappings.add("events", overrides);
        }
        if (!aliases.isEmpty()) {
            mappings.add("aliases", aliases);
        }
    }

    private boolean locateAudio(Event event) throws IOException {
        for (Sample sample : event.samples()) {
            Key key = sample.id();
            if (!this.audio.containsKey(key)) {
                Path path = this.pack.soundResource(key.namespace(), "sounds/" + key.value() + ".ogg", this.customFiles.contains(key));
                this.audio.put(key, path);
            }
            if (this.audio.get(key) == null) {
                return false;
            }
        }
        return true;
    }

    private Event resolve(Key id) throws IOException {
        Event cached = this.cache.get(id);
        if (cached != null) {
            return cached;
        }
        if (!this.visiting.add(id)) {
            return MISSING;
        }
        try {
            Event event = this.read(id);
            this.cache.put(id, event);
            return event;
        } finally {
            this.visiting.remove(id);
        }
    }

    private Event read(Key id) throws IOException {
        JsonObject vanilla = this.source.definitions(id.namespace(), false).getAsJsonObject(id.value());
        Key effective = this.restored.getOrDefault(id, id);
        JsonObject custom = this.source.definitions(effective.namespace(), true).getAsJsonObject(effective.value());
        boolean carrier = !effective.equals(id);
        if (carrier && Objects.equals(JavaSoundEvents.sounds(custom), JavaSoundEvents.sounds(OriginalEvents.ALL.getAsJsonObject(id.value())))) {
            custom = null;
        }
        if (vanilla == null && custom == null) {
            return MISSING;
        }
        boolean changed = custom != null;
        JsonArray entries = new JsonArray();
        if (custom == null || (!carrier && (!custom.has("replace") || !custom.get("replace").getAsBoolean()))) {
            entries.addAll(JavaSoundEvents.sounds(vanilla));
        }
        entries.addAll(JavaSoundEvents.sounds(custom));
        boolean missing = false;
        List<Sample> samples = new ArrayList<>();
        for (JsonElement entry : entries) {
            Key key = JavaSoundEvents.sampleId(entry);
            JsonObject modifiers = entry.isJsonObject() ? entry.getAsJsonObject() : null;
            Sample sample = new Sample(key, number(modifiers, "volume"), number(modifiers, "pitch"), number(modifiers, "weight"));
            if (JavaSoundEvents.isReference(entry)) {
                Event child = this.resolve(key);
                changed |= child.changed();
                missing |= child.missing();
                for (Sample leaf : child.samples()) {
                    samples.add(new Sample(leaf.id(), sample.volume() * leaf.volume(), sample.pitch() * leaf.pitch(), leaf.weight()));
                }
            } else {
                changed |= this.customFiles.contains(key);
                samples.add(sample);
            }
        }
        return new Event(changed, missing, samples);
    }

    private static double number(JsonObject object, String name) {
        return object != null && object.has(name) ? object.get(name).getAsDouble() : 1;
    }

    private static final class OriginalEvents {
        private static final JsonObject ALL = GsonHelper.readJsonFromResource("/internal/sounds.json").getAsJsonObject();
    }

    private record Event(boolean changed, boolean missing, List<Sample> samples) {
    }

    private record Sample(Key id, double volume, double pitch, double weight) {
        JsonElement encode(String path) {
            if (this.volume == 1 && this.pitch == 1 && this.weight == 1) {
                return new com.google.gson.JsonPrimitive(path);
            }
            JsonObject value = new JsonObject();
            value.addProperty("name", path);
            if (this.volume != 1) {
                value.addProperty("volume", this.volume);
            }
            if (this.pitch != 1) {
                value.addProperty("pitch", this.pitch);
            }
            if (this.weight != 1) {
                value.addProperty("weight", (int) this.weight);
            }
            return value;
        }
    }
}
