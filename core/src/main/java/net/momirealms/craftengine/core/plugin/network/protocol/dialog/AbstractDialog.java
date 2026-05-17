package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.function.Function;

public abstract class AbstractDialog implements Dialog {
    protected final DialogCommonData commonData;

    protected AbstractDialog(DialogCommonData commonData) {
        this.commonData = commonData;
    }

    public DialogCommonData commonData() {
        return this.commonData;
    }

    public void writeCommonData(CompoundTag tag) {
        this.commonData.write(tag);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.commonData.applyClientboundData(function);
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        this.commonData.replaceNetworkTags(function);
    }
}
