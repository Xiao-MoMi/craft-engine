package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;
import java.util.function.Function;

public final class ButtonCommonData {
    private Component label;
    private Optional<Component> tooltip;
    private final int width;

    public ButtonCommonData(Component label, Optional<Component> tooltip, int width) {
        this.label = label;
        this.tooltip = tooltip;
        this.width = width;
    }

    public static ButtonCommonData read(CompoundTag tag) {
        Component label = AdventureHelper.nbtToComponent(tag.get("label"));
        Optional<Component> tooltip = Optional.ofNullable(tag.get("tooltip")).map(AdventureHelper::nbtToComponent);
        int width = tag.getInt("width", 150);
        return new ButtonCommonData(label, tooltip, width);
    }

    public void replaceNetworkTags(Function<Component, Component> function) {
        this.label = function.apply(this.label);
        if (this.tooltip.isPresent()) {
            this.tooltip = Optional.of(function.apply(this.tooltip.get()));
        }
    }

    public void save(CompoundTag tag) {
        tag.put("label", AdventureHelper.componentToTag(this.label));
        this.tooltip.ifPresent(it -> tag.put("tooltip", AdventureHelper.componentToTag(it)));
        if (this.width != 150) {
            tag.putInt("width", this.width);
        }
    }

    public Component label() {
        return this.label;
    }

    public Optional<Component> tooltip() {
        return this.tooltip;
    }

    public int width() {
        return this.width;
    }
}
