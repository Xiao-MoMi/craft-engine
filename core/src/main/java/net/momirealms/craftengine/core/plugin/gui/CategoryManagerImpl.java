package net.momirealms.craftengine.core.plugin.gui;

import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.nio.file.Path;
import java.util.*;

public class CategoryManagerImpl implements CategoryManager {
    private final CraftEngine plugin;
    private final Map<Key, Category> byId;
    private final TreeSet<Category> ordered;

    public CategoryManagerImpl(CraftEngine plugin) {
        this.plugin = plugin;
        this.byId = new HashMap<>();
        this.ordered = new TreeSet<>();
    }

    @Override
    public void unload() {
        this.byId.clear();
        this.ordered.clear();
    }

    public void delayedLoad() {
        this.ordered.addAll(this.byId.values());
    }

    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        String name = section.getOrDefault("name", id).toString();
        List<String> members = MiscUtils.getAsStringList(section.getOrDefault("list", List.of()));
        Key icon = Key.of(section.getOrDefault("icon", ItemKeys.STONE).toString());
        int priority = MiscUtils.getAsInt(section.getOrDefault("priority", 0));
        Category category = new Category(id, name, icon, members, priority);
        if (this.byId.containsKey(id)) {
            this.byId.get(id).merge(category);
        } else {
            this.byId.put(id, category);
        }
    }

    @Override
    public TreeSet<Category> categories() {
        return ordered;
    }

    @Override
    public Optional<Category> byId(Key key) {
        return Optional.ofNullable(this.byId.get(key));
    }
}
