package net.momirealms.craftengine.bedrock.geyser.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.SoundMapping;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class GeyserSoundMappings {
    private final Map<String, SoundMapping> previous = new HashMap<>();
    private final Map<String, SoundMapping> installed = new HashMap<>();

    public void install(@Nullable JsonObject data) {
        if (data == null) {
            return;
        }
        this.uninstall();
        Map<String, SoundMapping> registry = Registries.SOUNDS.get();
        JsonObject events = data.getAsJsonObject("events");
        if (events != null) {
            for (Map.Entry<String, JsonElement> entry : events.entrySet()) {
                String id = entry.getKey();
                this.add(registry, id, named(id, entry.getValue().getAsString()));
            }
        }
        JsonObject aliases = data.getAsJsonObject("aliases");
        if (aliases != null) {
            for (Map.Entry<String, JsonElement> entry : aliases.entrySet()) {
                String original = entry.getValue().getAsString();
                SoundMapping mapping = registry.get(original);
                this.add(registry, entry.getKey(), mapping == null ? named(entry.getKey(), original) : mapping);
            }
        }
    }

    private static SoundMapping named(String id, String name) {
        return new SoundMapping(id, null, name, -1, null, false, 1);
    }

    private void add(Map<String, SoundMapping> registry, String id, SoundMapping mapping) {
        SoundMapping replaced = registry.put(id, mapping);
        if (!this.installed.containsKey(id) || this.installed.get(id) != replaced) {
            this.previous.put(id, replaced);
        }
        this.installed.put(id, mapping);
    }

    public void uninstall() {
        if (this.installed.isEmpty()) {
            return;
        }
        Map<String, SoundMapping> registry = Registries.SOUNDS.get();
        this.installed.forEach((id, mapping) -> {
            if (registry.get(id) == mapping) {
                SoundMapping original = this.previous.get(id);
                if (original == null) {
                    registry.remove(id);
                } else {
                    registry.put(id, original);
                }
            }
        });
        this.installed.clear();
        this.previous.clear();
    }
}
