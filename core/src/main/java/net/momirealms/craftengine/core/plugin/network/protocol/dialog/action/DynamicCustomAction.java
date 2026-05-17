package net.momirealms.craftengine.core.plugin.network.protocol.dialog.action;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.Optional;

public final class DynamicCustomAction implements DialogAction {
    private final Key id;
    private final Optional<CompoundTag> additions;

    public DynamicCustomAction(Key id, Optional<CompoundTag> additions) {
        this.id = id;
        this.additions = additions;
    }

    public static DynamicCustomAction read(CompoundTag tag) {
        return new DynamicCustomAction(
                Key.of(tag.getString("id")),
                Optional.of(tag.getCompound("additions"))
        );
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "dynamic/custom");
        tag.putString("id", this.id.toString());
        this.additions.ifPresent(compoundTag -> tag.put("additions", compoundTag));
        return tag;
    }

    public Key id() {
        return this.id;
    }

    public Optional<CompoundTag> additions() {
        return this.additions;
    }
}
