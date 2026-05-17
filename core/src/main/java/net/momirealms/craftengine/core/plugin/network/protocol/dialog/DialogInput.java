package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.input.DialogInputControl;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.input.DialogInputControlTypes;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.function.Function;

public final class DialogInput {
    private final String key;
    private final DialogInputControl control;

    public DialogInput(String key, DialogInputControl control) {
        this.key = key;
        this.control = control;
    }

    public void replaceNetworkTags(Function<Component, Component> function) {
        this.control.replaceNetworkTags(function);
    }

    public static DialogInput read(CompoundTag tag) {
        String key = tag.getString("key");
        return new DialogInput(key, DialogInputControlTypes.read(tag));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("key", this.key);
        this.control.write(tag);
        return tag;
    }

    public String key() {
        return this.key;
    }

    public DialogInputControl control() {
        return this.control;
    }
}
