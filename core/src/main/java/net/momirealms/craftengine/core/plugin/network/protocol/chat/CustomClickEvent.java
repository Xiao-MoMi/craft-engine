package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Optional;

public final class CustomClickEvent implements ClickEvent {
    private final Key id;
    private final Optional<Tag> payload;

    public CustomClickEvent(Key id, Optional<Tag> payload) {
        this.id = id;
        this.payload = payload;
    }

    public static CustomClickEvent read(CompoundTag compoundTag) {
        return new CustomClickEvent(
                Key.of(compoundTag.getString("id")),
                Optional.ofNullable(compoundTag.get("payload"))
        );
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "custom");
        tag.putString("id", this.id.asString());
        this.payload.ifPresent(it -> tag.put("payload", it));
        return tag;
    }

    public Key id() {
        return this.id;
    }

    public Optional<Tag> payload() {
        return this.payload;
    }

    @Override
    public Type<?> type() {
        return ClickEventTypes.CUSTOM;
    }
}
