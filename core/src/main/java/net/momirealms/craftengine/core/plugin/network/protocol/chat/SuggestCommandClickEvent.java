package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import net.momirealms.sparrow.nbt.CompoundTag;

public final class SuggestCommandClickEvent implements ClickEvent {
    private final String command;

    public SuggestCommandClickEvent(String command) {
        this.command = command;
    }

    public static SuggestCommandClickEvent read(CompoundTag compoundTag) {
        return new SuggestCommandClickEvent(compoundTag.getString("command"));
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "suggest_command");
        tag.putString("command", this.command);
        return tag;
    }

    public String command() {
        return this.command;
    }

    @Override
    public Type<?> type() {
        return ClickEventTypes.SUGGEST_COMMAND;
    }
}
