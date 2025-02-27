package net.momirealms.craftengine.core.plugin.gui.category;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Category implements Comparable<Category> {
    private final Key id;
    private final String displayName;
    private final Key icon;
    private final List<Key> members;
    private final int priority;

    public Category(Key id, String displayName, Key icon, List<Key> members, int priority) {
        this.id = id;
        this.displayName = displayName;
        this.members = new ArrayList<>(members);
        this.icon = icon;
        this.priority = priority;
    }

    public void addMember(Key member) {
        if (!this.members.contains(member)) {
            this.members.add(member);
        }
    }

    public Key id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Key icon() {
        return icon;
    }

    public List<Key> members() {
        return members;
    }

    public void merge(Category other) {
        for (Key member : other.members) {
            addMember(member);
        }
    }

    @Override
    public int compareTo(@NotNull Category o) {
        if (this.priority != o.priority) {
            return this.priority - o.priority;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(this.id.toString(), o.id.toString());
    }
}
