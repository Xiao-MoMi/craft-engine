package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import net.momirealms.sparrow.nbt.CompoundTag;

public final class CopyToClipboardClickEvent implements ClickEvent {
    private final String value;

    public CopyToClipboardClickEvent(String value) {
        this.value = value;
    }

    public static CopyToClipboardClickEvent read(CompoundTag compoundTag) {
        return new CopyToClipboardClickEvent(compoundTag.getString("value"));
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "copy_to_clipboard");
        tag.putString("value", this.value);
        return tag;
    }

    public String value() {
        return this.value;
    }

    @Override
    public Type<?> type() {
        return ClickEventTypes.COPY_TO_CLIPBOARD;
    }
}
