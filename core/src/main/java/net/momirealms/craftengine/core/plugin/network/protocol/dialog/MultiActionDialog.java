package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class MultiActionDialog extends AbstractDialog {
    private final List<ActionButton> actions;
    private final Optional<ActionButton> exit;
    private final int columns;

    public MultiActionDialog(DialogCommonData commonData, List<ActionButton> actions, Optional<ActionButton> exit, int columns) {
        super(commonData);
        this.actions = actions;
        this.exit = exit;
        this.columns = columns;
    }

    public static MultiActionDialog read(CompoundTag tag) {
        DialogCommonData commonData = DialogCommonData.read(tag);
        ListTag actionsTag = tag.getList("actions");
        List<ActionButton> actions = new ArrayList<>(actionsTag.size());
        for (int i = 0; i < actionsTag.size(); i++) {
            actions.add(ActionButton.read(actionsTag.getCompound(i)));
        }
        Optional<ActionButton> exit = Optional.ofNullable(tag.get("exit_action")).map(it -> ActionButton.read(((CompoundTag) it)));
        int columns = tag.getInt("columns", 2);
        return new MultiActionDialog(commonData, actions, exit, columns);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "multi_action");
        super.writeCommonData(tag);
        ListTag actionsTag = new ListTag();
        for (ActionButton action : this.actions) {
            actionsTag.add(action.save());
        }
        tag.put("actions", actionsTag);
        this.exit.ifPresent(action -> tag.put("exit_action", action.save()));
        if (this.columns != 2) {
            tag.putInt("columns", this.columns);
        }
        return tag;
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        super.applyClientboundData(function);
        for (ActionButton action : this.actions) {
            action.applyClientboundData(function);
        }
        this.exit.ifPresent(action -> action.applyClientboundData(function));
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        super.replaceNetworkTags(function);
        for (ActionButton action : this.actions) {
            action.replaceNetworkTags(function);
        }
        this.exit.ifPresent(action -> action.replaceNetworkTags(function));
    }

    public List<ActionButton> actions() {
        return this.actions;
    }

    public Optional<ActionButton> exit() {
        return this.exit;
    }

    public int columns() {
        return this.columns;
    }
}
