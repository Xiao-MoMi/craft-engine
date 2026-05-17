package net.momirealms.craftengine.core.plugin.network.protocol.dialog;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.body.DialogBody;
import net.momirealms.craftengine.core.plugin.network.protocol.dialog.body.DialogBodyTypes;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class DialogCommonData {
    private Component title;
    private Optional<Component> externalTitle;
    private final boolean canCloseWithEscape;
    private final boolean pause;
    private final AfterAction afterAction;
    private final List<DialogBody> body;
    private final List<DialogInput> inputs;

    public DialogCommonData(Component title,
                            Optional<Component> externalTitle,
                            boolean canCloseWithEscape,
                            boolean pause,
                            AfterAction afterAction,
                            List<DialogBody> body,
                            List<DialogInput> inputs) {
        this.title = title;
        this.externalTitle = externalTitle;
        this.canCloseWithEscape = canCloseWithEscape;
        this.pause = pause;
        this.afterAction = afterAction;
        this.body = body;
        this.inputs = inputs;
    }

    public void replaceNetworkTags(Function<Component, Component> function) {
        this.title = function.apply(this.title);
        if (this.externalTitle.isPresent()) {
            this.externalTitle = Optional.of(function.apply(this.externalTitle.get()));
        }
        for (DialogBody body : this.body) {
            body.replaceNetworkTags(function);
        }
        for (DialogInput input : this.inputs) {
            input.replaceNetworkTags(function);
        }
    }

    public void applyClientboundData(Function<Item, Item> function) {
        for (DialogBody body : this.body) {
            body.applyClientboundData(function);
        }
    }

    public static DialogCommonData read(final CompoundTag tag) {
        Component title = AdventureHelper.nbtToComponent(tag.get("title"));
        Optional<Component> externalTitle = Optional.ofNullable(tag.get("external_title")).map(AdventureHelper::nbtToComponent);
        boolean canCloseWithEscape = tag.getBoolean("can_close_with_escape", true);
        boolean pause = tag.getBoolean("pause", true);
        AfterAction afterAction = AfterAction.read(tag.get("after_action"));
        Tag bodyTag = tag.get("body");
        List<DialogBody> bodies;
        if (bodyTag instanceof ListTag bodyList) {
            bodies = new ArrayList<>(bodyList.size());
            for (int i = 0; i < bodyList.size(); i++) {
                bodies.add(DialogBodyTypes.read(bodyList.getCompound(i)));
            }
        } else if (bodyTag instanceof CompoundTag bodyCompound) {
            bodies = List.of(DialogBodyTypes.read(bodyCompound));
        } else {
            bodies = Collections.emptyList();
        }
        ListTag inputList = tag.getList("inputs");
        List<DialogInput> inputs;
        if (inputList == null) {
            inputs = List.of();
        } else {
            inputs = new ArrayList<>(inputList.size());
            for (int i = 0; i < inputList.size(); i++) {
                inputs.add(DialogInput.read(inputList.getCompound(i)));
            }
        }
        return new DialogCommonData(title, externalTitle, canCloseWithEscape, pause, afterAction, bodies, inputs);
    }

    public void write(CompoundTag tag) {
        tag.put("title", AdventureHelper.componentToTag(this.title));
        this.externalTitle.ifPresent(it -> tag.put("external_title", AdventureHelper.componentToTag(it)));
        if (!this.canCloseWithEscape) {
            tag.putBoolean("can_close_with_escape", false);
        }
        if (!this.pause) {
            tag.putBoolean("pause", false);
        }
        if (this.afterAction != AfterAction.CLOSE) {
            tag.putString("after_action", this.afterAction.id());
        }
        if (!this.body.isEmpty()) {
            if (this.body.size() == 1) {
                tag.put("body", this.body.getFirst().save());
            } else {
                ListTag listTag = new ListTag();
                for (DialogBody body : this.body) {
                    listTag.add(body.save());
                }
                tag.put("body", listTag);
            }
        }
        if (!this.inputs.isEmpty()) {
            ListTag listTag = new ListTag();
            for (DialogInput input : this.inputs) {
                listTag.add(input.save());
            }
            tag.put("inputs", listTag);
        }
    }

    public Component title() {
        return this.title;
    }

    public Optional<Component> externalTitle() {
        return this.externalTitle;
    }

    public boolean canCloseWithEscape() {
        return this.canCloseWithEscape;
    }

    public boolean pause() {
        return this.pause;
    }

    public AfterAction afterAction() {
        return this.afterAction;
    }

    public List<DialogBody> body() {
        return this.body;
    }

    public List<DialogInput> inputs() {
        return this.inputs;
    }
}
