package net.momirealms.craftengine.core.pack.bedrock.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.block.BlockSounds;
import net.momirealms.craftengine.core.pack.bedrock.JavaResourcePack;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.HashUtils;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.SOUND_DEFINITIONS;

public final class BedrockSounds {
    private static final JsonObject REGISTRY = GsonHelper.readJsonFromResource("/bedrock/vanilla_block_sounds.json").getAsJsonObject();
    private static final Map<Group, String> VANILLA = vanillaGroups();
    private final JavaSoundEvents events;
    private final JavaResourcePack resources;
    private final Map<Key, Key> replacements;
    private final Map<Group, String> groups = new LinkedHashMap<>();
    private final Map<String, JavaSoundEvents.Event> customEvents = new HashMap<>();
    private final Map<String, Set<String>> missing = new TreeMap<>();

    public BedrockSounds(JavaResourcePack resources, Map<Key, Key> replacements) {
        this.resources = resources;
        this.replacements = replacements;
        this.events = new JavaSoundEvents(resources, REGISTRY.getAsJsonObject("events"), replacements);
    }

    private static Map<Group, String> vanillaGroups() {
        Map<Group, String> groups = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : REGISTRY.getAsJsonObject("groups").entrySet()) {
            JsonArray ids = entry.getValue().getAsJsonArray();
            groups.put(new Group(ids.get(0).getAsString(), ids.get(1).getAsString(), ids.get(2).getAsString(),
                    ids.get(3).getAsString(), ids.get(4).getAsString()), entry.getKey());
        }
        return Map.copyOf(groups);
    }

    public Group resolve(BlockSounds sounds, Key block) throws IOException {
        return new Group(this.event(sounds.breakSound().id(), block, "break"), this.event(sounds.stepSound().id(), block, "step"),
                this.event(sounds.placeSound().id(), block, "place"), this.event(sounds.hitSound().id(), block, "hit"),
                this.event(sounds.fallSound().id(), block, "fall"));
    }

    private String event(Key id, Key block, String action) throws IOException {
        JavaSoundEvents.Event event = this.events.resolve(id);
        if (event == JavaSoundEvents.MISSING) {
            this.missing.computeIfAbsent(id.asString(), ignored -> new TreeSet<>()).add(block.asString() + "/" + action);
        } else if (!event.vanilla()) {
            this.customEvents.putIfAbsent(event.name(), event);
        }
        return event.name();
    }

    public String register(Group group) {
        String vanilla = VANILLA.get(group);
        if (vanilla != null) {
            return vanilla;
        }
        return this.groups.computeIfAbsent(group, key -> "ce:block_" + hash(String.join("\n", key.events())));
    }

    public void write(Path root, JsonObject playback) throws IOException {
        this.missing.forEach((sound, uses) -> TranslationManager.warn("resource_pack.bedrock.block.sound_missing", sound, String.join(", ", uses)));
        JsonObject blocks = new JsonObject();
        JsonObject movement = new JsonObject();
        JsonObject definitions = new JsonObject();
        Map<Key, String> copied = new HashMap<>();
        // Only materialize sounds reached from a successfully registered world block.
        for (Map.Entry<Group, String> entry : this.groups.entrySet()) {
            Group group = entry.getKey();
            JsonObject actions = new JsonObject();
            actions.addProperty("break", group.breakSound());
            actions.addProperty("place", group.place());
            actions.addProperty("hit", group.hit());
            actions.addProperty("default", "");
            JsonObject steps = new JsonObject();
            steps.addProperty("default", "");
            steps.addProperty("step", group.step());
            steps.addProperty("fall", group.fall());
            steps.addProperty("jump", group.step());
            steps.addProperty("land", group.fall());
            blocks.add(entry.getValue(), GsonHelper.createObject("events", actions));
            movement.add(entry.getValue(), GsonHelper.createObject("events", steps));
            for (String id : group.events()) {
                JavaSoundEvents.Event event = this.customEvents.get(id);
                if (event != null && !definitions.has(id)) {
                    definitions.add(id, this.writeEvent(root, event, copied));
                }
            }
        }
        JsonObject file = GsonHelper.createObject("block_sounds", blocks);
        file.add("interactive_sounds", GsonHelper.createObject("block_sounds", movement));
        if (!this.groups.isEmpty()) {
            GsonHelper.writeJsonFile(file, root.resolve("sounds.json"));
        }
        new PlaybackSounds(this.resources, this.events, this.replacements).write(root, playback, definitions, copied);
        if (!definitions.isEmpty()) {
            Files.createDirectories(root.resolve("sounds"));
            JsonObject sounds = GsonHelper.createObject("sound_definitions", definitions);
            sounds.addProperty(FORMAT_VERSION, SOUND_DEFINITIONS);
            GsonHelper.writeJsonFile(sounds, root.resolve("sounds/sound_definitions.json"));
        }
    }

    private JsonObject writeEvent(Path root, JavaSoundEvents.Event event, Map<Key, String> copied) throws IOException {
        JsonArray files = new JsonArray();
        for (Key sample : event.samples()) {
            files.add(copyAudio(root, sample, this.events.audio(sample), copied));
        }
        JsonObject definition = GsonHelper.createObject("sounds", files);
        definition.addProperty("category", "block");
        return definition;
    }

    private static String hash(String name) {
        return HashUtils.sha1(name.getBytes(StandardCharsets.UTF_8));
    }

    static String copyAudio(Path root, Key sample, Path source, Map<Key, String> copied) throws IOException {
        String path = copied.get(sample);
        if (path == null) {
            path = BedrockPackLayout.CRAFTENGINE_SOUNDS + '/' + hash(sample.asString());
            Path target = root.resolve(path + ".ogg");
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            copied.put(sample, path);
        }
        return path;
    }

    public record Group(String breakSound, String step, String place, String hit, String fall) {
        List<String> events() {
            return List.of(this.breakSound, this.step, this.place, this.hit, this.fall);
        }
    }
}
