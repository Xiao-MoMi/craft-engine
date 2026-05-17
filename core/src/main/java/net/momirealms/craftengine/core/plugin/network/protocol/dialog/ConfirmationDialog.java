package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.function.Function;

public final class ConfirmationDialog extends AbstractDialog {
    private final ActionButton yes;
    private final ActionButton no;

    public ConfirmationDialog(DialogCommonData commonData, ActionButton yes, ActionButton no) {
        super(commonData);
        this.yes = yes;
        this.no = no;
    }

    public static ConfirmationDialog read(CompoundTag tag) {
        DialogCommonData commonData = DialogCommonData.read(tag);
        ActionButton yes = ActionButton.read(tag.getCompound("yes"));
        ActionButton no = ActionButton.read(tag.getCompound("no"));
        return new ConfirmationDialog(commonData, yes, no);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "confirmation");
        super.writeCommonData(tag);
        tag.put("yes", this.yes.save());
        tag.put("no", this.no.save());
        return tag;
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        super.applyClientboundData(function);
        this.yes.applyClientboundData(function);
        this.no.applyClientboundData(function);
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        super.replaceNetworkTags(function);
        this.yes.replaceNetworkTags(function);
        this.no.replaceNetworkTags(function);
    }

    public ActionButton yes() {
        return this.yes;
    }

    public ActionButton no() {
        return this.no;
    }
}
