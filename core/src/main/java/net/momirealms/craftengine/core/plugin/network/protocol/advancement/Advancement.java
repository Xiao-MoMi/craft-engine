package net.momirealms.craftengine.core.plugin.network.protocol.advancement;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class Advancement {
    private final Optional<Key> parent;
    private final Optional<AdvancementDisplay> displayInfo;

    // 1.20-1.20.1
    private final Map<String, Void> criteria;

    private final AdvancementRequirements requirements;
    private final boolean sendsTelemetryEvent;

    public Advancement(Optional<Key> parent, Optional<AdvancementDisplay> displayInfo, AdvancementRequirements requirements, boolean sendsTelemetryEvent) {
        this.criteria = null;
        this.displayInfo = displayInfo;
        this.parent = parent;
        this.requirements = requirements;
        this.sendsTelemetryEvent = sendsTelemetryEvent;
    }

    @ApiStatus.Obsolete
    public Advancement(Optional<Key> parent, Optional<AdvancementDisplay> displayInfo, Map<String, Void> criteria, AdvancementRequirements requirements, boolean sendsTelemetryEvent) {
        this.criteria = criteria;
        this.displayInfo = displayInfo;
        this.parent = parent;
        this.requirements = requirements;
        this.sendsTelemetryEvent = sendsTelemetryEvent;
    }

    public static Advancement read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        Optional<Key> parent = buf.readOptional(FriendlyByteBuf::readKey);
        Optional<AdvancementDisplay> displayInfo = buf.readOptional(byteBuf -> AdvancementDisplay.read(buf, reader));
        if (VersionHelper.isOrAbove1_20_2) {
            AdvancementRequirements requirements = AdvancementRequirements.read(buf);
            boolean sendsTelemetryEvent = buf.readBoolean();
            return new Advancement(parent, displayInfo, requirements, sendsTelemetryEvent);
        } else {
            Map<String, Void> criteria = buf.readMap(FriendlyByteBuf::readUtf, (byteBuf -> null));
            AdvancementRequirements requirements = AdvancementRequirements.read(buf);
            boolean sendsTelemetryEvent = buf.readBoolean();
            return new Advancement(parent, displayInfo, criteria, requirements, sendsTelemetryEvent);
        }
    }

    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeOptional(this.parent, FriendlyByteBuf::writeKey);
        buf.writeOptional(this.displayInfo, (byteBuf, info) -> info.write(buf, writer));
        if (!VersionHelper.isOrAbove1_20_2) {
            buf.writeMap(this.criteria, FriendlyByteBuf::writeUtf, ((byteBuf, unused) -> {}));
        }
        this.requirements.write(buf);
        buf.writeBoolean(this.sendsTelemetryEvent);
    }

    public void applyClientboundData(Function<Item, Item> function) {
        this.displayInfo.ifPresent(info -> info.applyClientboundData(function));
    }

    public void replaceNetworkTags(Function<Component, Component> function) {
        this.displayInfo.ifPresent(info -> info.replaceNetworkTags(function));
    }
}
