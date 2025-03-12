/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.plugin.gui.category;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

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

    public Category(Key id, String displayName, List<String> displayLore, Key icon, List<String> members, int priority, boolean hidden) {
        this.id = id;
        this.displayName = displayName;
        this.members = new ArrayList<>(members);
        this.icon = icon;
        this.priority = priority;
        this.displayLore = new ArrayList<>(displayLore);
        this.hidden = hidden;
    }

    public void addMember(String member) {
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

    public boolean hidden() {
        return hidden;
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
