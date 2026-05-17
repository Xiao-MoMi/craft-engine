package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import net.momirealms.sparrow.nbt.CompoundTag;

public final class ChangePageClickEvent implements ClickEvent {
    private final int page;

    public ChangePageClickEvent(int page) {
        this.page = page;
    }

    public static ChangePageClickEvent read(CompoundTag compoundTag) {
        return new ChangePageClickEvent(compoundTag.getInt("page"));
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "change_page");
        tag.putInt("page", this.page);
        return tag;
    }

    public int page() {
        return this.page;
    }

    @Override
    public Type<?> type() {
        return ClickEventTypes.CHANGE_PAGE;
    }
}
