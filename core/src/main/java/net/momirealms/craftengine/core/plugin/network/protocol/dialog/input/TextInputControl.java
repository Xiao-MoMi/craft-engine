package net.momirealms.craftengine.core.plugin.network.protocol.dialog.input;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;
import java.util.function.Function;

public final class TextInputControl implements DialogInputControl {
    private final int width;
    private Component label;
    private final boolean labelVisible;
    private final String initial;
    private final int maxLength;
    private final Optional<MultilineOptions> multilineOptions;

    public TextInputControl(int width, Component label, boolean labelVisible, String initial, int maxLength, Optional<MultilineOptions> multilineOptions) {
        this.width = width;
        this.label = label;
        this.labelVisible = labelVisible;
        this.initial = initial;
        this.maxLength = maxLength;
        this.multilineOptions = multilineOptions;
    }

    public static TextInputControl read(CompoundTag tag) {
        int width = tag.getInt("width", 200);
        Component label = AdventureHelper.nbtToComponent(tag.get("label"));
        boolean labelVisible = tag.getBoolean("label_visible", true);
        String initial = tag.getString("initial", "");
        int maxLength = tag.getInt("max_length", 32);
        Optional<MultilineOptions> options = Optional.ofNullable(tag.get("multiline")).map(it -> MultilineOptions.read((CompoundTag) it));
        return new TextInputControl(width, label, labelVisible, initial, maxLength, options);
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putString("type", "text");
        tag.put("label", AdventureHelper.componentToTag(this.label));
        if (this.width != 200) {
            tag.putInt("width", this.width);
        }
        if (!this.labelVisible) {
            tag.putBoolean("label_visible", false);
        }
        if (this.maxLength != 32) {
            tag.putInt("max_length", this.maxLength);
        }
        if (!this.initial.isEmpty()) {
            tag.putString("initial", this.initial);
        }
        this.multilineOptions.ifPresent(it -> tag.put("multiline", it.save()));
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        this.label = function.apply(label);
    }

    public int width() {
        return this.width;
    }

    public Component label() {
        return this.label;
    }

    public boolean labelVisible() {
        return this.labelVisible;
    }

    public String initial() {
        return this.initial;
    }

    public int maxLength() {
        return this.maxLength;
    }

    public Optional<MultilineOptions> multilineOptions() {
        return this.multilineOptions;
    }

    public static class MultilineOptions {
        private final Optional<Integer> maxLines;
        private final Optional<Integer> height;

        public MultilineOptions(Optional<Integer> maxLines, Optional<Integer> height) {
            this.maxLines = maxLines;
            this.height = height;
        }

        public static MultilineOptions read(CompoundTag tag) {
            Optional<Integer> maxLines = tag.containsKey("max_lines") ? Optional.of(tag.getInt("max_lines")) : Optional.empty();
            Optional<Integer> height = tag.containsKey("height") ? Optional.of(tag.getInt("height")) : Optional.empty();
            return new MultilineOptions(maxLines, height);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            this.maxLines.ifPresent(maxLines -> tag.putInt("max_lines", maxLines));
            this.height.ifPresent(height -> tag.putInt("height", height));
            return tag;
        }

        public Optional<Integer> maxLines() {
            return this.maxLines;
        }

        public Optional<Integer> height() {
            return this.height;
        }
    }
}
