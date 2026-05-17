package net.momirealms.craftengine.core.plugin.network.protocol.dialog.input;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class SingleOptionInputControl implements DialogInputControl {
    private final int width;
    private final List<Entry> entries;
    private Component label;
    private final boolean labelVisible;

    public SingleOptionInputControl(int width, List<Entry> entries, Component label, boolean labelVisible) {
        this.width = width;
        this.entries = entries;
        this.label = label;
        this.labelVisible = labelVisible;
    }

    public static SingleOptionInputControl read(CompoundTag tag) {
        int width = tag.getInt("width", 200);
        List<Entry> entries = new ArrayList<>();
        ListTag optionsTag = tag.getList("options");
        for (int i = 0; i < optionsTag.size(); i++) {
            entries.add(Entry.read(optionsTag.get(i), i == 0));
        }
        Component label = AdventureHelper.nbtToComponent(tag.get("label"));
        boolean labelVisible = tag.getBoolean("label_visible", true);
        return new SingleOptionInputControl(width, entries, label, labelVisible);
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putString("type", "single_option");
        tag.put("label", AdventureHelper.componentToTag(this.label));
        if (this.width != 200) {
            tag.putInt("width", this.width);
        }
        ListTag entriesTag = new ListTag();
        for (Entry entry : this.entries) {
            entriesTag.add(entry.save());
        }
        tag.put("options", entriesTag);
        if (!this.labelVisible) {
            tag.putBoolean("label_visible", false);
        }
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        this.label = function.apply(label);
        for (Entry entry : this.entries) {
            entry.replaceNetworkTags(function);
        }
    }

    public int width() {
        return this.width;
    }

    public List<Entry> entries() {
        return this.entries;
    }

    public Component label() {
        return this.label;
    }

    public boolean labelVisible() {
        return this.labelVisible;
    }

    public static class Entry {
        private final String id;
        private Optional<Component> display;
        private final boolean initial;

        public Entry(String id, Optional<Component> display, boolean initial) {
            this.id = id;
            this.display = display;
            this.initial = initial;
        }

        public void replaceNetworkTags(Function<Component, Component> function) {
            if (this.display.isPresent()) {
                this.display = Optional.of(function.apply(this.display.get()));
            }
        }

        public static Entry read(Tag tag, boolean first) {
            if (tag instanceof StringTag stringTag) {
                return new Entry(stringTag.value(), Optional.empty(), false);
            }
            CompoundTag compoundTag = (CompoundTag) tag;
            return new Entry(
                    compoundTag.getString("id"),
                    Optional.ofNullable(compoundTag.get("display")).map(AdventureHelper::nbtToComponent),
                    compoundTag.getBoolean("initial", first)
            );
        }

        public Tag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", this.id);
            this.display.ifPresent(component -> tag.put("display", AdventureHelper.componentToTag(component)));
            if (this.initial) {
                tag.putBoolean("initial", true);
            }
            return tag;
        }

        public String id() {
            return this.id;
        }

        public Optional<Component> display() {
            return this.display;
        }

        public boolean initial() {
            return this.initial;
        }
    }
}
