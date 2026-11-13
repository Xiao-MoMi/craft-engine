package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard.FixedNumberProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard.ObjectivePacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard.ScorePacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard.TeamPacketProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureComponentProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureKeyProxy;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.display.slot.DisplaySlot;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.BlankFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.StyledFormat;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SidebarTestBoard {
    public static final String OBJECTIVE = "ce_sidebar_test";
    private static final String TEAM = "ce_sidebar_layer";
    private final Scoreboard scoreboard;
    private final Map<ScoreboardPosition, String> previous = new EnumMap<>(ScoreboardPosition.class);

    public SidebarTestBoard(GeyserSession session) {
        this.scoreboard = session.getWorldCache().getScoreboard();
        if (this.scoreboard.getObjective(OBJECTIVE) != null || this.scoreboard.getTeam(TEAM) != null) {
            throw new IllegalStateException("Test objective/team already exists; clear the previous test first");
        }
        for (DisplaySlot slot : this.scoreboard.getObjectiveSlots().values()) {
            if (DisplaySlot.slotCategory(slot.position()) == ScoreboardPosition.SIDEBAR) {
                this.previous.put(slot.position(), slot.objective().getObjectiveName());
            }
        }
    }

    public void show(GeyserSession session) {
        for (ScoreboardPosition position : this.previous.keySet()) {
            translate(session, new ClientboundSetDisplayObjectivePacket(position, ""));
        }
        translate(session, ObjectivePacketProxy.INSTANCE.create(OBJECTIVE, ObjectiveAction.ADD, text("CE Sidebar Test", "minecraft:default"), ScoreType.INTEGER, BlankFormat.INSTANCE));
        this.line(session, "text", 15, text("Text: English 中文 100%", "minecraft:default"), null);
        this.line(session, "icon", 14, parts(text("Icon ", "minecraft:default"), text("\ue001", "craftengine_test:sidebar")), null);
        this.line(session, "spacing", 13, parts(text("A", "minecraft:default"), text("\ue004", "craftengine_test:sidebar"), text("B", "minecraft:default")), null);
        Object empty = text("", "minecraft:default");
        Object prefix = text("\ue003\ue002", "craftengine_test:sidebar");
        translate(session, TeamPacketProxy.INSTANCE.create(TEAM, empty, prefix, text("Overlay", "minecraft:default"), false, false, NameTagVisibility.ALWAYS, CollisionRule.ALWAYS, null, new String[]{"layer"}));
        this.line(session, "layer", 12, empty, null);
        this.line(session, "empty-a", 11, empty, null);
        this.line(session, "empty-b", 10, empty, null);
        this.line(session, "tie-a", 9, text("Tie A", "minecraft:default"), null);
        this.line(session, "tie-b", 9, text("Tie B", "minecraft:default"), null);
        this.line(session, "number", 8, text("Numeric", "minecraft:default"), new StyledFormat(NbtMap.builder().putString("color", "gold").build()));
        this.line(session, "fixed", 7, text("Fixed", "minecraft:default"), FixedNumberProxy.INSTANCE.create(text("OK", "minecraft:default")));
        for (int i = 0; i < 7; i++) {
            this.line(session, "limit-" + i, 6 - i, text("Limit " + i, "minecraft:default"), null);
        }
        translate(session, new ClientboundSetDisplayObjectivePacket(ScoreboardPosition.SIDEBAR, OBJECTIVE));
        this.scoreboard.onUpdate();
    }

    public void update(GeyserSession session) {
        this.line(session, "text", 15, text("Updated 中文 100%", "minecraft:default"), null);
        translate(session, new ClientboundResetScorePacket("icon", OBJECTIVE));
        this.line(session, "tie-b", 16, text("Moved to first", "minecraft:default"), null);
        translate(session, ObjectivePacketProxy.INSTANCE.create(OBJECTIVE, ObjectiveAction.UPDATE, text("CE Sidebar Test - updated", "minecraft:default"), ScoreType.INTEGER, BlankFormat.INSTANCE));
        this.scoreboard.onUpdate();
    }

    public void clear(GeyserSession session) {
        if (session.getWorldCache().getScoreboard() != this.scoreboard) {
            return;
        }
        translate(session, new ClientboundSetObjectivePacket(OBJECTIVE));
        translate(session, new ClientboundSetPlayerTeamPacket(TEAM));
        for (Map.Entry<ScoreboardPosition, String> entry : this.previous.entrySet()) {
            if (this.scoreboard.getObjective(entry.getValue()) != null) {
                translate(session, new ClientboundSetDisplayObjectivePacket(entry.getKey(), entry.getValue()));
            }
        }
        this.scoreboard.onUpdate();
    }

    private void line(GeyserSession session, String owner, int score, Object display, NumberFormat format) {
        ClientboundSetScorePacket packet = new ClientboundSetScorePacket(owner, OBJECTIVE, score).withNumberFormat(format);
        translate(session, ScorePacketProxy.INSTANCE.withDisplay(packet, display));
    }

    private static Object text(String text, String font) {
        return AdventureComponentProxy.INSTANCE.font(AdventureComponentProxy.INSTANCE.text(text), AdventureKeyProxy.INSTANCE.key(font));
    }

    private static Object parts(Object... parts) {
        return AdventureComponentProxy.INSTANCE.children(AdventureComponentProxy.INSTANCE.text(""), List.of(parts));
    }

    @SuppressWarnings("unchecked")
    private static <P extends Packet> void translate(GeyserSession session, P packet) {
        PacketTranslator<P> translator = (PacketTranslator<P>) Registries.JAVA_PACKET_TRANSLATORS.get(packet.getClass());
        translator.translate(session, packet);
    }
}
