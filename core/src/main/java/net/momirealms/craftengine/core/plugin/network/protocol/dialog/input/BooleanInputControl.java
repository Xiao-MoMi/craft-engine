package net.momirealms.craftengine.core.plugin.network.protocol.dialog.input;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.function.Function;

public final class BooleanInputControl implements DialogInputControl {
    private Component label;
    private final boolean initial;
    private final String onTrue;
    private final String onFalse;

    public BooleanInputControl(Component label, boolean initial, String onTrue, String onFalse) {
        this.label = label;
        this.initial = initial;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    public static BooleanInputControl read(CompoundTag tag) {
        Component label = AdventureHelper.nbtToComponent(tag.get("label"));
        boolean initial = tag.getBoolean("initial");
        String onTrue = tag.getString("on_true", "true");
        String onFalse = tag.getString("on_false", "false");
        return new BooleanInputControl(label, initial, onTrue, onFalse);
    }

    @Override
    public void write(CompoundTag tag) {
        tag.putString("type", "boolean");
        tag.put("label", AdventureHelper.componentToTag(this.label));
        if (this.initial) {
            tag.putBoolean("initial", true);
        }
        if (!"true".equals(this.onTrue)) {
            tag.putString("on_true", this.onTrue);
        }
        if (!"false".equals(this.onFalse)) {
            tag.putString("on_false", this.onFalse);
        }
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        this.label = function.apply(label);
    }

    public Component label() {
        return this.label;
    }

    public boolean initial() {
        return this.initial;
    }

    public String onTrue() {
        return this.onTrue;
    }

    public String onFalse() {
        return this.onFalse;
    }
}
