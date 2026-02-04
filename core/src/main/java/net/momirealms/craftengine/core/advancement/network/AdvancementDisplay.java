package net.momirealms.craftengine.core.advancement.network;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.advancement.AdvancementType;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

import java.util.Optional;
import java.util.function.Function;

public final class AdvancementDisplay<I> {
    public static final int FLAG_BACKGROUND = 0b001;
    public static final int FLAG_SHOW_TOAST = 0b010;
    public static final int FLAG_HIDDEN = 0b100;
    private Component title;
    private Component description;
    private Item<I> icon;
    private Optional<Key> background;
    private final AdvancementType type;
    private final boolean showToast;
    private final boolean hidden;
    private float x;
    private float y;

    public AdvancementDisplay(Component title,
                              Component description,
                              Item<I> icon,
                              Optional<Key> background,
                              AdvancementType type,
                              boolean showToast,
                              boolean hidden,
                              float x,
                              float y) {
        this.type = type;
        this.showToast = showToast;
        this.hidden = hidden;
        this.background = background;
        this.description = description;
        this.icon = icon;
        this.title = title;
        this.x = x;
        this.y = y;
    }

    public void applyClientboundData(Function<Item<I>, Item<I>> function) {
        this.icon = function.apply(this.icon);
    }

    public void replaceNetworkTags(Function<Component, Component> function) {
        this.title = function.apply(this.title);
        this.description = function.apply(this.description);
    }

    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item<I>> writer) {
        buf.writeComponent(this.title);
        buf.writeComponent(this.description);
        writer.accept(buf, this.icon);
        buf.writeVarInt(this.type.ordinal());
        int flags = 0;
        if (this.background.isPresent()) {
            flags |= FLAG_BACKGROUND;
        }
        if (this.showToast) {
            flags |= FLAG_SHOW_TOAST;
        }
        if (this.hidden) {
            flags |= FLAG_HIDDEN;
        }
        buf.writeInt(flags);
        this.background.ifPresent(buf::writeKey);
        buf.writeFloat(this.x);
        buf.writeFloat(this.y);
    }

    public static <I> AdvancementDisplay<I> read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item<I>> reader) {
        Component title = buf.readComponent();
        Component description = buf.readComponent();
        Item<I> icon = reader.apply(buf);
        AdvancementType type = AdvancementType.byId(buf.readVarInt());
        int flags = buf.readInt();
        boolean hasBackground = (flags & 1) != 0;
        Optional<Key> background = hasBackground ? Optional.of(buf.readKey()) : Optional.empty();
        boolean showToast = (flags & 2) != 0;
        boolean hidden = (flags & 4) != 0;
        float x = buf.readFloat();
        float y = buf.readFloat();
        return new AdvancementDisplay<>(title, description, icon, background, type, showToast, hidden, x, y);
    }
}
