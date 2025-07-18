package net.momirealms.craftengine.core.item.recipe.network.modern.display.slot;

import com.mojang.datafixers.util.Either;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.NotNull;

public class SmithingTrimDemoSlotDisplay implements SlotDisplay {
    private final SlotDisplay base;
    private final SlotDisplay material;
    // 1.21.2-1.21.4
    private SlotDisplay trimPattern;
    // 1.21.5
    private Either<Integer, TrimPattern> either;

    public SmithingTrimDemoSlotDisplay(SlotDisplay base, SlotDisplay material, SlotDisplay trimPattern) {
        this.base = base;
        this.material = material;
        this.trimPattern = trimPattern;
    }

    public SmithingTrimDemoSlotDisplay(SlotDisplay base, SlotDisplay material, Either<Integer, TrimPattern> either) {
        this.base = base;
        this.either = either;
        this.material = material;
    }

    public static SmithingTrimDemoSlotDisplay read(FriendlyByteBuf buf) {
        SlotDisplay base = SlotDisplay.read(buf);
        SlotDisplay material = SlotDisplay.read(buf);
        if (VersionHelper.isOrAbove1_21_5()) {
            Either<Integer, TrimPattern> either = buf.readHolder(byteBuf -> {
                Key assetId = buf.readKey();
                Component component = AdventureHelper.nbtToComponent(buf.readNbt(false));
                boolean decal = buf.readBoolean();
                return new TrimPattern(assetId, component, decal);
            });
            return new SmithingTrimDemoSlotDisplay(base, material, either);
        } else {
            SlotDisplay trimPattern = SlotDisplay.read(buf);
            return new SmithingTrimDemoSlotDisplay(base, material, trimPattern);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(5);
        this.base.write(buf);
        this.material.write(buf);
        if (VersionHelper.isOrAbove1_21_5()) {
            buf.writeHolder(this.either, (byteBuf, pattern) -> {
                byteBuf.writeKey(pattern.assetId);
                byteBuf.writeNbt(AdventureHelper.componentToNbt(pattern.description), false);
                byteBuf.writeBoolean(pattern.decal);
            });
        } else {
            this.trimPattern.write(buf);
        }
    }

    @Override
    public String toString() {
        return "SmithingTrimDemoSlotDisplay{" +
                "base=" + base +
                ", material=" + material +
                ", trimPattern=" + trimPattern +
                ", either=" + either +
                '}';
    }

    public record TrimPattern(Key assetId, Component description, boolean decal) {

        @Override
        public @NotNull String toString() {
            return "TrimPattern{" +
                    "assetId=" + assetId +
                    ", description=" + description +
                    ", decal=" + decal +
                    '}';
        }
    }
}
