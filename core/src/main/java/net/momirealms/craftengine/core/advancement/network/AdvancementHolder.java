package net.momirealms.craftengine.core.advancement.network;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

public record AdvancementHolder(Key id, Advancement advancement) {

    public static AdvancementHolder read(FriendlyByteBuf buf) {
        Key key = buf.readKey();
        Advancement ad = Advancement.read(buf);
        return new AdvancementHolder(key, ad);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeKey(this.id);
        this.advancement.write(buf);
    }

    public void applyClientboundData(Player player) {
        this.advancement.applyClientboundData(player);
    }
}
