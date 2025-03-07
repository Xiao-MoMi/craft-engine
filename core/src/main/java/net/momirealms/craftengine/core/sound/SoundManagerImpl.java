package net.momirealms.craftengine.core.sound;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.nio.file.Path;
import java.util.*;

public class SoundManagerImpl implements SoundManager {
    private final CraftEngine plugin;
    private final Map<Key, SoundEvent> byId;
    private final Map<String, List<SoundEvent>> byNamespace;

    public SoundManagerImpl(CraftEngine plugin) {
        this.plugin = plugin;
        this.byId = new HashMap<>();
        this.byNamespace = new HashMap<>();
    }

    @Override
    public void unload() {
        this.byId.clear();
        this.byNamespace.clear();
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        if (this.byId.containsKey(id)) {
            this.plugin.logger().warn(path, "Sound " + id + " already exists");
            return;
        }
        boolean replace = (boolean) section.getOrDefault("replace", false);
        String subtitle = (String) section.get("subtitle");
        List<?> soundList = (List<?>) section.get("sounds");
        List<Sound> sounds = new ArrayList<>();
        for (Object sound : soundList) {
            if (sound instanceof String soundPath) {
                sounds.add(Sound.path(soundPath));
            } else if (sound instanceof Map<?,?> map) {
                sounds.add(Sound.SoundFile.fromMap(MiscUtils.castToMap(map, false)));
            }
        }
        SoundEvent event = new SoundEvent(id, replace, subtitle, sounds);
        this.byId.put(id, event);
        this.byNamespace.computeIfAbsent(id.namespace(), k -> new ArrayList<>()).add(event);
    }

    @Override
    public Map<Key, SoundEvent> sounds() {
        return Collections.unmodifiableMap(this.byId);
    }

    public Map<String, List<SoundEvent>> soundsByNamespace() {
        return Collections.unmodifiableMap(this.byNamespace);
    }
}
