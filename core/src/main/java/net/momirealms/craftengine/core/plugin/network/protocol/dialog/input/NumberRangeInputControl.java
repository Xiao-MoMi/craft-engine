package net.momirealms.craftengine.core.plugin.network.protocol.dialog.input;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;
import java.util.function.Function;

public final class NumberRangeInputControl implements DialogInputControl {
    private final int width;
    private Component label;
    private final String labelFormat;
    private final RangeInfo rangeInfo;

    public NumberRangeInputControl(int width, Component label, String labelFormat, RangeInfo rangeInfo) {
        this.width = width;
        this.label = label;
        this.labelFormat = labelFormat;
        this.rangeInfo = rangeInfo;
    }

    public static NumberRangeInputControl read(CompoundTag tag) {
        int width = tag.getInt("width", 200);
        Component label = AdventureHelper.nbtToComponent(tag.get("label"));
        String labelFormat = tag.getString("label_format", "options.generic_value");
        RangeInfo rangeInfo = RangeInfo.read(tag);
        return new NumberRangeInputControl(width, label, labelFormat, rangeInfo);
    }

    @Override
    public void write(CompoundTag tag) {
        tag.put("label", AdventureHelper.componentToTag(this.label));
        if (this.width != 200) {
            tag.putInt("width", this.width);
        }
        if (!"options.generic_value".equals(this.labelFormat)) {
            tag.putString("label_format", this.labelFormat);
        }
        this.rangeInfo.write(tag);
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

    public String labelFormat() {
        return this.labelFormat;
    }

    public RangeInfo rangeInfo() {
        return this.rangeInfo;
    }

    public static class RangeInfo {
        private final float start;
        private final float end;
        private final Optional<Float> initial;
        private final Optional<Float> step;

        public RangeInfo(float start, float end, Optional<Float> initial, Optional<Float> step) {
            this.start = start;
            this.end = end;
            this.initial = initial;
            this.step = step;
        }

        public static RangeInfo read(CompoundTag tag) {
            float start = tag.getFloat("start");
            float end = tag.getFloat("end");
            Optional<Float> initial = tag.containsKey("initial") ? Optional.of(tag.getFloat("initial")) : Optional.empty();
            Optional<Float> step = tag.containsKey("step") ? Optional.of(tag.getFloat("step")) : Optional.empty();
            return new RangeInfo(start, end, initial, step);
        }

        public void write(CompoundTag tag) {
            tag.putString("type", "number_range");
            tag.putFloat("start", this.start);
            tag.putFloat("end", this.end);
            this.initial.ifPresent(i -> tag.putFloat("initial", i));
            this.step.ifPresent(i -> tag.putFloat("step", i));
        }

        public float start() {
            return this.start;
        }

        public float end() {
            return this.end;
        }

        public Optional<Float> initial() {
            return this.initial;
        }

        public Optional<Float> step() {
            return this.step;
        }
    }
}
