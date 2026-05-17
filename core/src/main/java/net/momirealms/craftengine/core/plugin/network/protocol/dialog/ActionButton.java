package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.action.DialogAction;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.action.DialogActionTypes;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;
import java.util.function.Function;

public final class ActionButton {
    private final ButtonCommonData commonData;
    private final Optional<DialogAction> action;

    public ActionButton(ButtonCommonData commonData, Optional<DialogAction> action) {
        this.commonData = commonData;
        this.action = action;
    }

    public static ActionButton read(CompoundTag tag) {
        ButtonCommonData commonData = ButtonCommonData.read(tag);
        Optional<DialogAction> action = Optional.ofNullable(tag.get("action")).map(it -> DialogActionTypes.read((CompoundTag) it));
        return new ActionButton(commonData, action);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        this.commonData.save(tag);
        this.action.ifPresent(it -> {
            tag.put("action", it.save());
        });
        return tag;
    }

    public void applyClientboundData(Function<Item, Item> function) {
       this.action.ifPresent(it -> it.applyClientboundData(function));
    }

    public void replaceNetworkTags(Function<Component, Component> function) {
        this.commonData.replaceNetworkTags(function);
        this.action.ifPresent(it -> it.replaceNetworkTags(function));
    }

    public ButtonCommonData commonData() {
        return this.commonData;
    }

    public Optional<DialogAction> action() {
        return this.action;
    }
}
