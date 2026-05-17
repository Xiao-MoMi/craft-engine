package net.momirealms.craftengine.core.plugin.network.protocol.dialog.body;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Optional;
import java.util.function.Function;

public final class ItemBody implements DialogBody {
    private Item item;
    private final Optional<PlainMessageBody> description;
    private final boolean showDecorations;
    private final boolean showTooltip;
    private final int width;
    private final int height;

    public ItemBody(Item item, Optional<PlainMessageBody> description, boolean showDecorations, boolean showTooltip, int width, int height) {
        this.item = item;
        this.description = description;
        this.showDecorations = showDecorations;
        this.showTooltip = showTooltip;
        this.width = width;
        this.height = height;
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.item = function.apply(item);
    }

    @Override
    public void replaceNetworkTags(Function<Component, Component> function) {
        this.description.ifPresent(it -> it.replaceNetworkTags(function));
    }

    public static ItemBody read(CompoundTag tag) {
        Optional<PlainMessageBody> description;
        Tag descriptionTag = tag.get("description");
        if (descriptionTag != null) {
            if (descriptionTag instanceof CompoundTag descriptionCompoundTag && descriptionCompoundTag.containsKey("contents")) {
                description = Optional.of(PlainMessageBody.read(descriptionCompoundTag));
            } else {
                description = Optional.of(new PlainMessageBody(AdventureHelper.nbtToComponent(descriptionTag), 200));
            }
        } else {
            description = Optional.empty();
        }

        int width = tag.getInt("width", 16);
        int height = tag.getInt("height", 16);
        boolean showDecorations = tag.getBoolean("show_decoration", true);
        boolean showTooltip = tag.getBoolean("show_tooltip", true);
        Item item = Item.fromNBT(tag.getCompound("item"));
        return new ItemBody(item, description, showDecorations, showTooltip, width, height);
    }

    @Override
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "item");
        tag.put("item", this.item.toNBT());
        this.description.ifPresent(d -> {
            tag.put("description", d.save());
        });
        if (!this.showDecorations) {
            tag.putBoolean("show_decoration", false);
        }
        if (!this.showTooltip) {
            tag.putBoolean("show_tooltip", false);
        }
        if (this.width != 16) {
            tag.putInt("width", this.width);
        }
        if (this.height != 16) {
            tag.putInt("height", this.height);
        }
        return tag;
    }

    public Item item() {
        return this.item;
    }

    public Optional<PlainMessageBody> description() {
        return this.description;
    }

    public boolean showDecorations() {
        return this.showDecorations;
    }

    public boolean showTooltip() {
        return this.showTooltip;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }
}
