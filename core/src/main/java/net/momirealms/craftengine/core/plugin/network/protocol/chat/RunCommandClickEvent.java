package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import net.momirealms.sparrow.nbt.CompoundTag;

public final class RunCommandClickEvent implements ClickEvent {
    private final String command;

    public RunCommandClickEvent(String command) {
        this.command = command;
    }

    public static RunCommandClickEvent read(CompoundTag compoundTag) {
        return new RunCommandClickEvent(compoundTag.getString("command"));
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "run_command");
        tag.putString("command", this.command);
        return tag;
    }

    public String command() {
        return this.command;
    }

    @Override
    public Type<?> type() {
        return ClickEventTypes.RUN_COMMAND;
    }
}
