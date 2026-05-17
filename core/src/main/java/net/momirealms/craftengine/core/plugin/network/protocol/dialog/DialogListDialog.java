package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import com.mojang.datafixers.util.Either;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class DialogListDialog extends AbstractDialog {
    private final List<Either<String, Dialog>> dialogs;
    private final Optional<ActionButton> exit;
    private final int columns;
    private final int buttonWidth;

    public DialogListDialog(DialogCommonData commonData, List<Either<String, Dialog>> dialogs, Optional<ActionButton> exit, int columns, int buttonWidth) {
        super(commonData);
        this.dialogs = dialogs;
        this.exit = exit;
        this.columns = columns;
        this.buttonWidth = buttonWidth;
    }

    public static DialogListDialog read(CompoundTag tag) {
        DialogCommonData commonData = DialogCommonData.read(tag);
        List<Either<String, Dialog>> dialogs;
        Tag dialogsTag = tag.get("dialogs");
        if (dialogsTag instanceof StringTag stringTag) {
            dialogs = List.of(Either.left(stringTag.value()));
        } else if (dialogsTag instanceof CompoundTag compoundTag) {
            dialogs = List.of(Either.right(DialogTypes.read(compoundTag)));
        } else if (dialogsTag instanceof ListTag listTag) {
            dialogs = new ArrayList<>(listTag.size());
            for (Tag innerTag : listTag) {
                if (innerTag instanceof CompoundTag compoundTag) {
                    dialogs.add(Either.right(DialogTypes.read(compoundTag)));
                } else if (innerTag instanceof StringTag stringTag) {
                    dialogs.add(Either.left(stringTag.value()));
                }
            }
        } else {
            throw new IllegalArgumentException("Unexpected dialog tag: " + dialogsTag);
        }
        Optional<ActionButton> exit = Optional.ofNullable(tag.get("exit_action")).map(it -> ActionButton.read((CompoundTag) it));
        int columns = tag.getInt("columns", 2);
        int buttonWidth = tag.getInt("button_width", 150);
        return new DialogListDialog(commonData, dialogs, exit, columns, buttonWidth);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "dialog_list");
        super.writeCommonData(tag);
        ListTag dialogsTag = new ListTag();
        for (Either<String, Dialog> dialog : this.dialogs) {
            dialog.ifLeft(s -> dialogsTag.add(new StringTag(s)))
                    .ifRight(d -> dialogsTag.add(d.save()));
        }
        tag.put("dialogs", dialogsTag);
        this.exit.ifPresent(actionButton -> tag.put("exit_action", actionButton.save()));
        if (this.columns != 2) {
            tag.putInt("columns", this.columns);
        }
        if (this.buttonWidth != 150) {
            tag.putInt("button_width", this.buttonWidth);
        }
        return tag;
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        super.applyClientboundData(function);
        for (Either<String, Dialog> dialog : this.dialogs) {
            dialog.ifRight(d -> d.applyClientboundData(function));
        }
        this.exit.ifPresent(actionButton -> actionButton.applyClientboundData(function));
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        super.replaceNetworkTags(function);
        for (Either<String, Dialog> dialog : this.dialogs) {
            dialog.ifRight(d -> d.replaceNetworkTags(function));
        }
        this.exit.ifPresent(actionButton -> actionButton.replaceNetworkTags(function));
    }

    public List<Either<String, Dialog>> dialogs() {
        return this.dialogs;
    }

    public Optional<ActionButton> exit() {
        return this.exit;
    }

    public int columns() {
        return this.columns;
    }

    public int buttonWidth() {
        return this.buttonWidth;
    }
}
