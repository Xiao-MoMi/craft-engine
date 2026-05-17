package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;
import java.util.function.Function;

public final class ServerLinksDialog extends AbstractDialog {
    private final Optional<ActionButton> exit;
    private final int columns;
    private final int buttonWidth;

    public ServerLinksDialog(DialogCommonData commonData, Optional<ActionButton> exit, int columns, int buttonWidth) {
        super(commonData);
        this.exit = exit;
        this.columns = columns;
        this.buttonWidth = buttonWidth;
    }

    public static ServerLinksDialog read(CompoundTag tag) {
        DialogCommonData commonData = DialogCommonData.read(tag);
        Optional<ActionButton> exit = Optional.ofNullable(tag.get("exit_action")).map(it -> ActionButton.read((CompoundTag) it));
        int columns = tag.getInt("columns", 2);
        int buttonWidth = tag.getInt("button_width", 150);
        return new ServerLinksDialog(commonData, exit, columns, buttonWidth);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        super.applyClientboundData(function);
        this.exit.ifPresent(actionButton -> actionButton.applyClientboundData(function));
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        super.replaceNetworkTags(function);
        this.exit.ifPresent(actionButton -> actionButton.replaceNetworkTags(function));
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "server_links");
        super.writeCommonData(tag);
        this.exit.ifPresent(it -> {
            tag.put("exit_button", it.save());
        });
        if (this.columns != 2) {
            tag.putInt("columns", this.columns);
        }
        if (this.buttonWidth != 150) {
            tag.putInt("button_width", this.buttonWidth);
        }
        return tag;
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
