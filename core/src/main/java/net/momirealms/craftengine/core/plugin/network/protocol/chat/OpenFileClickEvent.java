package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import net.momirealms.sparrow.nbt.CompoundTag;

public final class OpenFileClickEvent implements ClickEvent {
    private final String path;

    public OpenFileClickEvent(String path) {
        this.path = path;
    }

    public static OpenFileClickEvent read(CompoundTag compoundTag) {
        return new OpenFileClickEvent(compoundTag.getString("path"));
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "open_file");
        tag.putString("path", path);
        return tag;
    }

    public String path() {
        return this.path;
    }

    @Override
    public Type<?> type() {
        return ClickEventTypes.OPEN_FILE;
    }
}
