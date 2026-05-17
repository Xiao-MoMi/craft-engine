package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;
import java.util.function.Function;

public final class NoticeDialog extends AbstractDialog {
    public static final ActionButton DEFAULT_ACTION = new ActionButton(
            new ButtonCommonData(Component.translatable("gui.ok"), Optional.empty(), 150),
            Optional.empty()
    );

    private final ActionButton action;

    public NoticeDialog(DialogCommonData commonData, ActionButton action) {
        super(commonData);
        this.action = action;
    }

    public static NoticeDialog read(CompoundTag tag) {
        DialogCommonData commonData = DialogCommonData.read(tag);
        ActionButton actionButton = tag.containsKey("action") ? ActionButton.read(tag.getCompound("action")) : DEFAULT_ACTION;
        return new NoticeDialog(commonData, actionButton);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "notice");
        super.writeCommonData(tag);
        tag.put("action", this.action.save());
        return tag;
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        super.applyClientboundData(function);
        this.action.applyClientboundData(function);
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        super.replaceNetworkTags(function);
        this.action.replaceNetworkTags(function);
    }

    public ActionButton button() {
        return this.action;
    }
}
