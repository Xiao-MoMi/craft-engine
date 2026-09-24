package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import com.mojang.datafixers.util.Either;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.List;

public final class TagSlotDisplay implements SlotDisplay {
    private final Either<List<Integer>, Key> tag;

    public TagSlotDisplay(Key tag) {
        this(Either.right(tag));
    }

    public TagSlotDisplay(Either<List<Integer>, Key> tag) {
        this.tag = tag;
    }

    public static TagSlotDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        // Since 26.3, tag displays carry a holder set (a named tag or explicit item IDs).
        return VersionHelper.isOrAbove26_3
                ? new TagSlotDisplay(buf.readHolderSet())
                : new TagSlotDisplay(buf.readKey());
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.TAG));
        if (VersionHelper.isOrAbove26_3) {
            buf.writeHolderSet(this.tag);
        } else {
            buf.writeKey(this.tag.right().orElseThrow());
        }
    }

    @Override
    public String toString() {
        return "TagSlotDisplay{" +
                "tag=" + this.tag +
                '}';
    }
}
