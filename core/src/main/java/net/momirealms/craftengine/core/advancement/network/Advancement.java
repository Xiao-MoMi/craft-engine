package net.momirealms.craftengine.core.advancement.network;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Optional;

public class Advancement {
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

    public static Advancement read(FriendlyByteBuf buf) {
        Optional<Key> parent = buf.readOptional(FriendlyByteBuf::readKey);
        Optional<AdvancementDisplay> displayInfo = buf.readOptional(byteBuf -> AdvancementDisplay.read(buf));
        if (VersionHelper.isOrAbove1_20_2()) {
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

    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(this.parent, FriendlyByteBuf::writeKey);
        buf.writeOptional(this.displayInfo, (byteBuf, info) -> info.write(buf));
        if (!VersionHelper.isOrAbove1_20_2()) {
            buf.writeMap(this.criteria, FriendlyByteBuf::writeUtf, ((byteBuf, unused) -> {}));
        }
        this.requirements.write(buf);
        buf.writeBoolean(this.sendsTelemetryEvent);
    }

    public void applyClientboundData(Player player) {
        this.displayInfo.ifPresent(info -> info.applyClientboundData(player));
    }
}
