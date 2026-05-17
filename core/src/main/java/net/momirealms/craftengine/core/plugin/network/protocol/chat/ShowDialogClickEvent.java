package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import com.mojang.datafixers.util.Either;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.Dialog;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.DialogTypes;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Objects;
import java.util.function.Function;

public final class ShowDialogClickEvent implements ClickEvent {
    private final Either<String, Dialog> dialog;

    public ShowDialogClickEvent(Either<String, Dialog> dialog) {
        this.dialog = dialog;
    }

    public static ShowDialogClickEvent read(CompoundTag compoundTag) {
        Tag dialogTag = Objects.requireNonNull(compoundTag.get("dialog"), "dialog tag is null");
        if (dialogTag instanceof StringTag stringTag) {
            return new ShowDialogClickEvent(Either.left(stringTag.value()));
        }
        return new ShowDialogClickEvent(Either.right(DialogTypes.read((CompoundTag) dialogTag)));
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.dialog.ifRight(dialog -> dialog.applyClientboundData(function));
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        this.dialog.ifRight(dialog -> dialog.replaceNetworkTags(function));
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "show_dialog");
        this.dialog.ifLeft(s -> tag.putString("dialog", s))
                .ifRight(d -> tag.put("dialog", d.save()));
        return tag;
    }


    public Either<String, Dialog> dialog() {
        return this.dialog;
    }

    @Override
    public Type<?> type() {
        return ClickEventTypes.SHOW_DIALOG;
    }
}
