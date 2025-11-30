package net.momirealms.craftengine.core.plugin.gui.category;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Category implements Comparable<Category> {
    private final Key id;
    private final String displayName;
    private final List<String> displayLore;
    private final Key icon;
    private final List<String> members;
    private final int priority;
    private final boolean hidden;
    private final Condition<Context> condition;

    public Category(Key id, String displayName, List<String> displayLore, Key icon, List<String> members, int priority, boolean hidden, Condition<Context> condition) {
        this.id = id;
        this.displayName = displayName;
        this.members = new ArrayList<>(members);
        this.icon = icon;
        this.priority = priority;
        this.displayLore = new ArrayList<>(displayLore);
        this.hidden = hidden;
        this.condition = condition;
    }

    public void addMember(String member) {
        this.members.add(member);
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

    public boolean hidden() {
        return hidden;
    }

    @Nullable
    public Condition<Context> condition() {
        return condition;
    }

    public List<String> displayLore() {
        return displayLore;
    }

    public List<String> members() {
        return members;
    }

    public void merge(Category other) {
        for (String member : other.members) {
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
