package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import net.momirealms.sparrow.nbt.CompoundTag;

public final class OpenUrlClickEvent implements ClickEvent {
    private final String url;

    public OpenUrlClickEvent(String url) {
        this.url = url;
    }

    public static OpenUrlClickEvent read(CompoundTag compoundTag) {
        return new OpenUrlClickEvent(compoundTag.getString("url"));
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "open_url");
        tag.putString("url", this.url);
        return tag;
    }

    public String url() {
        return this.url;
    }

    @Override
    public Type<?> type() {
        return ClickEventTypes.OPEN_URL;
    }
}
