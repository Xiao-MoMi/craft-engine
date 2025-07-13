package net.momirealms.craftengine.core.advancement.network;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class AdvancementRequirements {
    public static final AdvancementRequirements EMPTY = new AdvancementRequirements(List.of());
    private final List<List<String>> requirements;

    public AdvancementRequirements(List<List<String>> requirements) {
        this.requirements = requirements;
    }

    public void write(FriendlyByteBuf byteBuf) {
        byteBuf.writeCollection(this.requirements, ((buf, strings) -> buf.writeCollection(strings, FriendlyByteBuf::writeUtf)));
    }

    public static AdvancementRequirements read(FriendlyByteBuf byteBuf) {
        return new AdvancementRequirements(byteBuf.readCollection(ArrayList::new, buf -> buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf)));
    }
}
