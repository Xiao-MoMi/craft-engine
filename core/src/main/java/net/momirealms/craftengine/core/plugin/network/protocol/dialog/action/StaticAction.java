package net.momirealms.craftengine.core.plugin.network.protocol.dialog.action;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.chat.ClickEvent;
import net.momirealms.craftengine.core.plugin.network.protocol.chat.ClickEventTypes;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.function.Function;

public final class StaticAction implements DialogAction {
    private final ClickEvent value;

    public StaticAction(ClickEvent clickEvent) {
        this.value = clickEvent;
    }

    public static StaticAction read(CompoundTag tag) {
        ClickEvent event = ClickEventTypes.read(tag);
        return new StaticAction(event);
    }

    @Override
    public CompoundTag save() {
        return this.value.save();
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.value.applyClientboundData(function);
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        this.value.replaceNetworkTags(function);
    }

    public ClickEvent value() {
        return this.value;
    }
}
