package net.momirealms.craftengine.core.item.network;

import net.momirealms.craftengine.core.item.processor.ItemProcessors;
import net.momirealms.craftengine.core.item.processor.NetworkItemProcessor;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;

import java.util.*;

public final class GlobalNetworkItemProcessors {
    private final Map<Key, Entry> entries = new LinkedHashMap<>();
    private NetworkItemProcessor[] processors = new NetworkItemProcessor[0];

    public synchronized void register(Key id, int priority, NetworkItemProcessor processor) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(processor, "processor");
        if (this.entries.containsKey(id)) {
            throw new IllegalArgumentException("Global network item processor already registered: " + id);
        }
        this.entries.put(id, new Entry(priority, processor, false));
        this.rebuild();
    }

    public synchronized boolean unregister(Key id) {
        if (this.entries.remove(id) == null) {
            return false;
        }
        this.rebuild();
        return true;
    }

    public NetworkItemProcessor[] processors() {
        return this.processors;
    }

    public synchronized void loadConfig(List<ConfigSection> sections) {
        Map<Key, Entry> configured = new LinkedHashMap<>();
        for (ConfigSection section : sections) {
            int priority = section.getInt("priority", 100);
            ItemProcessors.collectNetworkProcessors(section.getNonNullSection("data"), processor -> {
                Key id = Key.ce("config_" + configured.size());
                Entry existing = this.entries.get(id);
                if (existing != null && !existing.fromConfig) {
                    throw new IllegalArgumentException("Global network item processor already registered: " + id);
                }
                configured.put(id, new Entry(priority, processor, true));
            });
        }
        // Parse first so an invalid configuration cannot partially replace the active processors.
        this.entries.values().removeIf(Entry::fromConfig);
        this.entries.putAll(configured);
        this.rebuild();
    }

    private void rebuild() {
        Entry[] sorted = this.entries.values().toArray(new Entry[0]);
        Arrays.sort(sorted, Comparator.comparingInt(Entry::priority));
        NetworkItemProcessor[] rebuilt = new NetworkItemProcessor[sorted.length];
        for (int i = 0; i < sorted.length; i++) {
            rebuilt[i] = sorted[i].processor;
        }
        this.processors = rebuilt;
    }

    private record Entry(int priority, NetworkItemProcessor processor, boolean fromConfig) {
    }
}
