package net.momirealms.craftengine.core.plugin.network.protocol.dialog.body;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.function.Function;

public final class PlainMessageBody implements DialogBody {
    private Component contents;
    private final int width;

    public PlainMessageBody(Component contents, int width) {
        this.contents = contents;
        this.width = width;
    }

    public static PlainMessageBody read(CompoundTag tag) {
        int width = tag.getInt("width", 200);
        Component contents = AdventureHelper.nbtToComponent(tag.get("contents"));
        return new PlainMessageBody(contents, width);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "plain_message");
        tag.put("contents", AdventureHelper.componentToTag(this.contents));
        if (this.width != 200) {
            tag.putInt("width", this.width);
        }
        return tag;
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        this.contents = function.apply(contents);
    }

    public Component contents() {
        return this.contents;
    }

    public int width() {
        return this.width;
    }
}
