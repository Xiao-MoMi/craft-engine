package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import com.google.common.collect.MapMaker;
import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphSerializer;
import net.momirealms.craftengine.bedrock.geyser.network.GeyserPacketEvent;
import net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard.*;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.*;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.SidebarCodec;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.scoreboard.*;
import org.geysermc.geyser.scoreboard.display.slot.DisplaySlot;
import org.geysermc.geyser.scoreboard.display.slot.SidebarDisplaySlot;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.*;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.*;

import java.util.*;

public final class SidebarSession {
    private final GeyserSession session;
    private final ContainerTitleLayout layout;
    private final SidebarProfiles profiles;
    private final Map<String, ObjectiveText> objectives = new HashMap<>();
    private final Map<String, TeamText> teams = new HashMap<>();
    private final Map<String, Map<String, String>> titles = new MapMaker().weakKeys().makeMap();
    private String displayed;
    private final Map<Long, ScoreInfo> sent = new HashMap<>();
    private final SidebarRowSnapshot rows = new SidebarRowSnapshot();
    private Scoreboard scoreboard;

    public SidebarSession(GeyserSession session, ContainerTitleLayout layout, SidebarProfiles profiles) {
        this.session = session;
        this.layout = layout;
        this.profiles = profiles;
    }

    public void translate(ClientboundSetObjectivePacket packet, PacketTranslator<ClientboundSetObjectivePacket> original) {
        this.checkScoreboard();
        String name = packet.getName();
        Objective current = this.scoreboard.getObjective(name);
        boolean accepted = packet.getAction() == ObjectiveAction.ADD ? current == null : current != null;
        if (accepted && packet.getAction() != ObjectiveAction.REMOVE) {
            ObjectiveText text = this.objectives.computeIfAbsent(name, key -> new ObjectiveText());
            text.title = ObjectivePacketProxy.INSTANCE.displayName(packet);
            if (current != null) {
                for (DisplaySlot slot : this.scoreboard.getObjectiveSlots().values()) {
                    if (slot instanceof SidebarDisplaySlot && slot.objective() == current) {
                        slot.markNeedsUpdate();
                    }
                }
                if (text.active && !Objects.equals(current.getNumberFormat(), packet.getNumberFormat())) {
                    this.refreshObjective(current, text, packet.getNumberFormat(), true);
                }
            }
        }
        original.translate(this.session, packet);
        if (accepted && packet.getAction() == ObjectiveAction.REMOVE) {
            this.objectives.remove(name);
        } else if (accepted) {
            this.bindTitle(this.scoreboard.getObjective(name), this.objectives.get(name));
        }
    }

    public void translate(ClientboundSetDisplayObjectivePacket packet, PacketTranslator<ClientboundSetDisplayObjectivePacket> original) {
        this.checkScoreboard();
        boolean sidebar = DisplaySlot.slotCategory(packet.getPosition()) == ScoreboardPosition.SIDEBAR;
        DisplaySlot previous = this.scoreboard.getObjectiveSlots().get(packet.getPosition());
        Objective next = this.scoreboard.getObjective(packet.getName());
        ObjectiveText text = this.objectives.get(packet.getName());
        if (sidebar && next != null && text != null && !text.active) {
            text.active = true;
            this.refreshObjective(next, text, next.getNumberFormat(), false);
        }
        original.translate(this.session, packet);
        if (sidebar && next != null && text != null) {
            this.bindTitle(next, text);
        }
        if (previous instanceof SidebarDisplaySlot && previous.objective() != next) {
            Objective removed = previous.objective();
            ObjectiveText old = this.objectives.get(removed.getObjectiveName());
            if (old != null && this.scoreboard.getObjectiveSlots().values().stream().noneMatch(slot -> slot instanceof SidebarDisplaySlot && slot.objective() == removed)) {
                old.active = false;
                this.refreshObjective(removed, old, removed.getNumberFormat(), false);
            }
        }
    }

    private void bindTitle(Objective objective, ObjectiveText text) {
        if (!text.active) {
            return;
        }
        List<GuiTitlePart> parts = new ArrayList<>();
        if (!this.collect(text.title, 0xffffff, parts)) {
            return;
        }
        int profile = this.profiles.match(objective.getObjectiveName(), plain(parts));
        String title = this.encode(parts, profile, null, "");
        if (title != null) {
            // Geyser 将槽位 ID 和标题字符串原样交给出站包。弱身份键随槽位和最后一个排队的包一起释放。
            for (DisplaySlot slot : this.scoreboard.getObjectiveSlots().values()) {
                if (slot instanceof SidebarDisplaySlot && slot.objective() == objective) {
                    this.titles.computeIfAbsent(slot.objectiveId(), id -> new MapMaker().weakKeys().makeMap())
                            .put(objective.getDisplayName(), title);
                }
            }
        }
    }

    public void translate(ClientboundSetScorePacket packet, PacketTranslator<ClientboundSetScorePacket> original) {
        this.checkScoreboard();
        ObjectiveText text = this.objectives.get(packet.getObjective());
        if (text != null && !packet.getOwner().startsWith("#")) {
            Object display = ScorePacketProxy.INSTANCE.display(packet);
            text.entries.put(packet.getOwner(), display);
            if (text.active) {
                Objective objective = this.scoreboard.getObjective(packet.getObjective());
                NumberFormat format = packet.getNumberFormat() == null ? objective.getNumberFormat() : packet.getNumberFormat();
                String row = this.row(packet.getOwner(), display, this.team(packet.getOwner()), format, packet.getValue());
                if (row != null) {
                    Object component = AdventureComponentProxy.INSTANCE.font(AdventureComponentProxy.INSTANCE.text(row), AdventureKeyProxy.INSTANCE.key(GeyserGlyphSerializer.RESOLVED_FONT));
                    packet = ScorePacketProxy.INSTANCE.withDisplay(packet, component);
                } else if (display == null) {
                    ScoreReference reference = objective.getScores().get(packet.getOwner());
                    if (reference != null && this.rows.isWrapped(reference.displayName())) {
                        ScoreReferenceProxy.INSTANCE.displayName(reference, null);
                        reference.markChanged();
                    }
                }
            }
        }
        original.translate(this.session, packet);
    }

    public void translate(ClientboundResetScorePacket packet, PacketTranslator<ClientboundResetScorePacket> original) {
        this.checkScoreboard();
        if (packet.getObjective() == null) {
            for (ObjectiveText text : this.objectives.values()) {
                text.entries.remove(packet.getOwner());
            }
        } else {
            ObjectiveText text = this.objectives.get(packet.getObjective());
            if (text != null) {
                text.entries.remove(packet.getOwner());
            }
        }
        original.translate(this.session, packet);
    }

    public void translate(ClientboundSetPlayerTeamPacket packet, PacketTranslator<ClientboundSetPlayerTeamPacket> original) {
        this.checkScoreboard();
        String name = packet.getTeamName();
        Team current = this.scoreboard.getTeam(name);
        boolean create = packet.getAction() == TeamAction.CREATE;
        if ((create && current == null) || (!create && current != null)) {
            switch (packet.getAction()) {
                case CREATE, UPDATE -> {
                    TeamText text = this.teamText(packet);
                    this.teams.put(name, text);
                    Iterable<String> members = current == null ? Arrays.asList(packet.getPlayers()) : current.entities();
                    this.refreshMembers(members, text);
                }
                case ADD_PLAYER -> this.refreshMembers(Arrays.asList(packet.getPlayers()), this.teams.get(name));
                case REMOVE_PLAYER -> {
                    for (String owner : packet.getPlayers()) {
                        if (this.scoreboard.getTeamFor(owner) == current) {
                            this.refreshMember(owner, null);
                        }
                    }
                }
                case REMOVE -> {
                    this.teams.remove(name);
                    this.refreshMembers(current.entities(), null);
                }
            }
        }
        original.translate(this.session, packet);
    }

    private TeamText teamText(ClientboundSetPlayerTeamPacket packet) {
        Object prefix = TeamPacketProxy.INSTANCE.prefix(packet);
        int color = 0xffffff;
        String legacyColor = "";
        if (packet.getColor() != null) {
            legacyColor = ChatColor.chatColorFor(packet.getColor());
            Object colored = LegacyComponentSerializerProxy.INSTANCE.deserialize(MessageTranslatorProxy.INSTANCE.bedrockSerializer(), legacyColor + "x");
            Object own = AdventureStyleProxy.INSTANCE.color(AdventureComponentProxy.INSTANCE.style(colored));
            if (own != null) {
                color = AdventureTextColorProxy.INSTANCE.value(own);
            }
        }
        int prefixLength = legacyColor.length() * 2 + MessageTranslatorProxy.INSTANCE.convertMessageRaw(prefix, this.session.locale()).length() + ChatColor.RESET.length();
        return new TeamText(prefix, TeamPacketProxy.INSTANCE.suffix(packet), color, prefixLength);
    }

    private TeamText team(String owner) {
        Team team = this.scoreboard.getTeamFor(owner);
        return team == null ? null : this.teams.get(team.id());
    }

    private void refreshMembers(Iterable<String> members, TeamText team) {
        for (String owner : members) {
            this.refreshMember(owner, team);
        }
    }

    private void refreshMember(String owner, TeamText team) {
        for (Map.Entry<String, ObjectiveText> entry : this.objectives.entrySet()) {
            ObjectiveText text = entry.getValue();
            if (text.active && text.entries.containsKey(owner)) {
                Objective objective = this.scoreboard.getObjective(entry.getKey());
                this.refresh(objective.getScores().get(owner), text.entries.get(owner), team, objective.getNumberFormat(), true);
            }
        }
    }

    private void refreshObjective(Objective objective, ObjectiveText text, NumberFormat format, boolean inheritedOnly) {
        for (Map.Entry<String, Object> entry : text.entries.entrySet()) {
            ScoreReference score = objective.getScores().get(entry.getKey());
            if (score != null && (!inheritedOnly || score.numberFormat() == null)) {
                this.refresh(score, entry.getValue(), this.team(entry.getKey()), format, text.active);
            }
        }
    }

    private void refresh(ScoreReference score, Object original, TeamText team, NumberFormat inherited, boolean active) {
        if (score == null) {
            return;
        }
        String name = null;
        if (active) {
            NumberFormat format = score.numberFormat() == null ? inherited : score.numberFormat();
            name = this.row(score.name(), original, team, format, score.score());
        }
        if (name == null && original != null) {
            name = MessageTranslatorProxy.INSTANCE.convertMessageRaw(original, this.session.locale());
        }
        if (!Objects.equals(name, score.displayName())) {
            ScoreReferenceProxy.INSTANCE.displayName(score, name);
            score.markChanged();
        }
    }

    private String row(String owner, Object display, TeamText team, NumberFormat format, int value) {
        if (display == null) {
            display = LegacyComponentSerializerProxy.INSTANCE.deserialize(MessageTranslatorProxy.INSTANCE.bedrockSerializer(), owner);
        }
        List<GuiTitlePart> parts = new ArrayList<>();
        int color = team == null ? 0xffffff : team.color;
        if ((team != null && !this.collect(team.prefix, color, parts)) || !this.collect(display, color, parts) || (team != null && !this.collect(team.suffix, color, parts))) {
            return null;
        }
        String encoded = this.encode(parts, 0, null, this.number(format, value));
        if (encoded == null) {
            return null;
        }
        return this.rows.wrap(encoded, team == null ? 0 : team.prefixLength);
    }

    public synchronized void onDisplay(GeyserPacketEvent<SetDisplayObjectivePacket> event) {
        SetDisplayObjectivePacket packet = event.packet();
        if (!"sidebar".equals(packet.getDisplaySlot())) {
            return;
        }
        if (!packet.getObjectiveId().equals(this.displayed)) {
            this.sent.clear();
            this.displayed = packet.getObjectiveId();
        }
        Map<String, String> versions = this.titles.get(packet.getObjectiveId());
        String nativeName = packet.getDisplayName();
        String name = versions == null ? nativeName : versions.getOrDefault(nativeName, nativeName);
        name = name.replace("%", "%%");
        if (!name.equals(nativeName)) {
            SetDisplayObjectivePacket replacement = packet.clone();
            replacement.setDisplayName(name);
            event.setPacket(replacement);
        }
    }

    public synchronized void onRemove(GeyserPacketEvent<RemoveObjectivePacket> event) {
        String id = event.packet().getObjectiveId();
        if (id.equals(this.displayed)) {
            this.displayed = null;
            this.sent.clear();
        }
    }

    public synchronized void onScores(GeyserPacketEvent<SetScorePacket> event) {
        SetScorePacket packet = event.packet();
        if (packet.getAction() == SetScorePacket.Action.REMOVE) {
            for (ScoreInfo score : packet.getInfos()) {
                this.sent.remove(score.getScoreboardId());
            }
            return;
        }
        List<ScoreInfo> rows = packet.getInfos();
        List<ScoreInfo> rewritten = null;
        SetScorePacket removed = null;
        for (int index = 0; index < rows.size(); index++) {
            ScoreInfo row = rows.get(index);
            String nativeName = row.getName();
            if (nativeName == null) {
                continue;
            }
            boolean visible = row.getObjectiveId().equals(this.displayed);
            String name = this.rows.wireName(nativeName, visible);
            if (!visible && name == nativeName) {
                continue;
            }
            if (!name.equals(nativeName)) {
                if (rewritten == null) {
                    rewritten = new ArrayList<>(rows);
                }
                row = new ScoreInfo(row.getScoreboardId(), row.getObjectiveId(), row.getScore(), name);
                rewritten.set(index, row);
            }
            if (!visible) {
                continue;
            }
            ScoreInfo previous = this.sent.put(row.getScoreboardId(), row);
            if (previous != null && !name.equals(previous.getName())) {
                if (removed == null) {
                    removed = new SetScorePacket();
                    removed.setAction(SetScorePacket.Action.REMOVE);
                }
                removed.getInfos().add(new ScoreInfo(row.getScoreboardId(), row.getObjectiveId(), 0));
            }
        }
        if (removed != null) {
            event.writeBefore(removed);
        }
        if (rewritten != null) {
            SetScorePacket replacement = packet.clone();
            replacement.setInfos(rewritten);
            event.setPacket(replacement);
        }
    }

    private void checkScoreboard() {
        Scoreboard current = this.session.getWorldCache().getScoreboard();
        if (current != this.scoreboard) {
            this.objectives.clear();
            this.teams.clear();
            this.scoreboard = current;
        }
    }

    public synchronized void close() {
        this.checkScoreboard();
        if (!this.session.isClosed()) {
            this.restoreActiveRows();
        }
        this.titles.clear();
        this.displayed = null;
        this.sent.clear();
        this.objectives.clear();
        this.teams.clear();
    }

    private void restoreActiveRows() {
        for (Map.Entry<String, ObjectiveText> entry : this.objectives.entrySet()) {
            Objective objective = this.scoreboard.getObjective(entry.getKey());
            if (entry.getValue().active && objective != null) {
                entry.getValue().active = false;
                this.refreshObjective(objective, entry.getValue(), objective.getNumberFormat(), false);
            }
        }
    }

    private String number(NumberFormat format, int value) {
        if (format instanceof BlankFormat) {
            return "";
        }
        Object component;
        if (format instanceof FixedFormat fixed) {
            component = FixedNumberProxy.INSTANCE.value(fixed);
        } else if (format instanceof StyledFormat styled) {
            component = MessageTranslatorProxy.INSTANCE.componentFromNbtTag(styled.getStyle().toBuilder().putString("text", Integer.toString(value)).build());
        } else {
            return "§r§c" + value;
        }
        return MessageTranslatorProxy.INSTANCE.convertMessage(component, this.session.locale());
    }

    private String encode(List<GuiTitlePart> parts, int profile, String order, String number) {
        try {
            ContainerTitleLayout.MeasuredLayout measured = this.layout.layoutWithWidth(parts);
            return SidebarCodec.encode(profile, order, measured.width(), number, measured.layers(),
                    layer -> GeyserTextParts.serializeLayer(layer, parts, this.session.locale()));
        } catch (UnsupportedOperationException exception) {
            return null;
        }
    }

    private boolean collect(Object component, int color, List<GuiTitlePart> parts) {
        return component != null && GeyserTextParts.collectParts(component, "minecraft:default", color, 0, parts, this.session.locale());
    }

    private static String plain(List<GuiTitlePart> parts) {
        StringBuilder result = new StringBuilder();
        for (GuiTitlePart part : parts) {
            result.append(part.text());
        }
        return result.toString();
    }

    public synchronized String describe() {
        this.checkScoreboard();
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, ObjectiveText> entry : this.objectives.entrySet()) {
            result.append(entry.getKey()).append(" = ").append(MessageTranslatorProxy.INSTANCE.convertMessage(entry.getValue().title, this.session.locale())).append('\n');
        }
        return result.isEmpty() ? "No Java objectives received" : result.toString();
    }

    private static final class ObjectiveText {
        private Object title;
        private boolean active;
        private final Map<String, Object> entries = new HashMap<>();
    }

    private record TeamText(Object prefix, Object suffix, int color, int prefixLength) {
    }
}
