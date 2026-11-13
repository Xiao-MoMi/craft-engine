package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.proxy.scoreboard.*;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.*;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import net.momirealms.craftengine.bedrock.text.SidebarCodec;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetScorePacket;
import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.ScoreReference;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.Team;
import org.geysermc.geyser.scoreboard.display.score.SidebarDisplayScore;
import org.geysermc.geyser.scoreboard.display.slot.DisplaySlot;
import org.geysermc.geyser.scoreboard.display.slot.SidebarDisplaySlot;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.BlankFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.FixedFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.NumberFormat;
import org.geysermc.mcprotocollib.protocol.data.game.chat.numbers.StyledFormat;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.ObjectiveAction;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundResetScorePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SidebarSession {
    private final GeyserSession session;
    private final ContainerTitleLayout layout;
    private final SidebarProfiles profiles;
    private final Map<String, ObjectiveText> objectives = new HashMap<>();
    private final Map<String, TeamText> teams = new HashMap<>();
    private final Map<Long, ScoreInfo> sent = new HashMap<>();
    private Scoreboard scoreboard;

    public SidebarSession(GeyserSession session, ContainerTitleLayout layout, SidebarProfiles profiles) {
        this.session = session;
        this.layout = layout;
        this.profiles = profiles;
    }

    public void capture(Packet packet) {
        // Retain components before Geyser serializes away fonts; identities and membership stay in Geyser.
        this.checkScoreboard();
        if (packet instanceof ClientboundSetObjectivePacket objective) {
            Objective current = this.scoreboard.getObjective(objective.getName());
            if (objective.getAction() == ObjectiveAction.REMOVE) {
                this.objectives.remove(objective.getName());
            } else if ((objective.getAction() == ObjectiveAction.ADD) == (current == null)) {
                ObjectiveText text = this.objectives.computeIfAbsent(objective.getName(), key -> new ObjectiveText());
                text.title = ObjectivePacketProxy.INSTANCE.displayName(objective);
                if (current != null) {
                    for (DisplaySlot slot : current.getActiveSlots()) {
                        slot.markNeedsUpdate();
                    }
                }
            }
        } else if (packet instanceof ClientboundSetScorePacket score) {
            ObjectiveText objective = this.objectives.get(score.getObjective());
            if (objective != null) {
                objective.entries.put(score.getOwner(), ScorePacketProxy.INSTANCE.display(score));
                Objective current = this.scoreboard.getObjective(score.getObjective());
                ScoreReference reference = current == null ? null : current.getScores().get(score.getOwner());
                if (reference != null) {
                    reference.markChanged();
                }
            }
        } else if (packet instanceof ClientboundResetScorePacket reset) {
            if (reset.getObjective() == null) {
                for (ObjectiveText objective : this.objectives.values()) {
                    objective.entries.remove(reset.getOwner());
                }
            } else {
                ObjectiveText objective = this.objectives.get(reset.getObjective());
                if (objective != null) {
                    objective.entries.remove(reset.getOwner());
                }
            }
        } else if (packet instanceof ClientboundSetPlayerTeamPacket team) {
            Team current = this.scoreboard.getTeam(team.getTeamName());
            if (team.getAction() == TeamAction.REMOVE) {
                this.teams.remove(team.getTeamName());
            } else if ((team.getAction() == TeamAction.CREATE && current == null) || (team.getAction() == TeamAction.UPDATE && current != null)) {
                this.teams.put(team.getTeamName(), new TeamText(TeamPacketProxy.INSTANCE.prefix(team), TeamPacketProxy.INSTANCE.suffix(team)));
                if (current != null) {
                    current.markChanged();
                }
            }
        }
    }

    private void checkScoreboard() {
        Scoreboard current = this.session.getWorldCache().getScoreboard();
        if (this.scoreboard != current) {
            this.objectives.clear();
            this.teams.clear();
            this.sent.clear();
            this.scoreboard = current;
        }
    }

    public synchronized void send(BedrockPacket packet, Consumer<BedrockPacket> output) {
        this.checkScoreboard();
        if (packet instanceof SetDisplayObjectivePacket objective && "sidebar".equals(objective.getDisplaySlot())) {
            SidebarDisplaySlot slot = this.slot(objective.getObjectiveId());
            if (slot != null) {
                ObjectiveText text = this.objectives.get(slot.objective().getObjectiveName());
                if (text != null && text.title != null) {
                    List<GuiTitlePart> parts = new ArrayList<>();
                    if (this.collect(text.title, 0xffffff, parts)) {
                        int profile = this.profiles.match(slot.objective().getObjectiveName(), plain(parts));
                        String encoded = this.encode(parts, profile, null, "");
                        if (encoded != null) {
                            objective = objective.clone();
                            objective.setDisplayName(encoded);
                            packet = objective;
                        }
                    }
                }
            }
            if (objective.getDisplayName().indexOf('%') >= 0) {
                objective = objective.clone();
                objective.setDisplayName(objective.getDisplayName().replace("%", "%%"));
                packet = objective;
            }
        } else if (packet instanceof SetScorePacket scores) {
            if (scores.getAction() == SetScorePacket.Action.REMOVE) {
                for (ScoreInfo score : scores.getInfos()) {
                    this.sent.remove(score.getScoreboardId());
                }
            } else {
                SetScorePacket rewritten = new SetScorePacket();
                rewritten.setAction(SetScorePacket.Action.SET);
                SetScorePacket removed = new SetScorePacket();
                removed.setAction(SetScorePacket.Action.REMOVE);
                // A font-only change can leave Geyser's legacy name unchanged, so compare the actual wire name too.
                for (ScoreInfo score : scores.getInfos()) {
                    ScoreInfo transformed = this.score(score);
                    // The Bedrock scoreboard controller formats percent signs before exposing UI bindings.
                    if (this.slot(score.getObjectiveId()) != null && transformed.getName() != null && transformed.getName().indexOf('%') >= 0) {
                        transformed = new ScoreInfo(transformed.getScoreboardId(), transformed.getObjectiveId(), transformed.getScore(), transformed.getName().replace("%", "%%"));
                    }
                    ScoreInfo previous = this.sent.put(score.getScoreboardId(), transformed);
                    if (previous != null && !java.util.Objects.equals(previous.getName(), transformed.getName())) {
                        removed.getInfos().add(new ScoreInfo(score.getScoreboardId(), score.getObjectiveId(), 0));
                    }
                    rewritten.getInfos().add(transformed);
                }
                if (!removed.getInfos().isEmpty()) {
                    output.accept(removed);
                }
                packet = rewritten;
            }
        } else if (packet instanceof RemoveObjectivePacket removed) {
            this.sent.values().removeIf(score -> score.getObjectiveId().equals(removed.getObjectiveId()));
        }
        output.accept(packet);
    }

    private ScoreInfo score(ScoreInfo info) {
        SidebarDisplaySlot slot = this.slot(info.getObjectiveId());
        if (slot == null) {
            return info;
        }
        ObjectiveText objective = this.objectives.get(slot.objective().getObjectiveName());
        if (objective == null) {
            return info;
        }
        for (SidebarDisplayScore score : SidebarSlotProxy.INSTANCE.scores(slot)) {
            if (score.cachedInfo() == null || score.cachedInfo().getScoreboardId() != info.getScoreboardId()) {
                continue;
            }
            Object display = objective.entries.get(score.name());
            if (display == null) {
                display = LegacyComponentSerializerProxy.INSTANCE.deserialize(MessageTranslatorProxy.INSTANCE.bedrockSerializer(), score.name());
            }
            List<GuiTitlePart> parts = new ArrayList<>();
            Team team = this.scoreboard.getTeamFor(score.name());
            TeamText teamText = team == null ? null : this.teams.get(team.id());
            int color = 0xffffff;
            if (team != null && team.color() != null) {
                Object colored = LegacyComponentSerializerProxy.INSTANCE.deserialize(MessageTranslatorProxy.INSTANCE.bedrockSerializer(), ChatColor.chatColorFor(team.color()) + "x");
                Object ownColor = AdventureStyleProxy.INSTANCE.color(AdventureComponentProxy.INSTANCE.style(colored));
                if (ownColor != null) {
                    color = AdventureTextColorProxy.INSTANCE.value(ownColor);
                }
            }
            if (teamText != null && !this.collect(teamText.prefix(), color, parts)) {
                return info;
            }
            if (!this.collect(display, color, parts) || (teamText != null && !this.collect(teamText.suffix(), color, parts))) {
                return info;
            }
            ScoreReference reference = slot.objective().getScores().get(score.name());
            NumberFormat format = reference.numberFormat() == null ? slot.objective().getNumberFormat() : reference.numberFormat();
            String number = this.number(format, reference.score());
            String encoded = this.encode(parts, 0, score.order(), number);
            return encoded == null ? info : new ScoreInfo(info.getScoreboardId(), info.getObjectiveId(), info.getScore(), encoded);
        }
        return info;
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
            List<List<ContainerTitleLayout.Run>> layers = this.layout.layout(parts, true);
            List<String> text = new ArrayList<>(layers.size());
            for (List<ContainerTitleLayout.Run> layer : layers) {
                text.add(GeyserTextParts.serializeLayer(layer, parts, this.session.locale()));
            }
            return SidebarCodec.encode(profile, order, this.layout.width(parts), number, text);
        } catch (UnsupportedOperationException exception) {
            return null;
        }
    }

    private boolean collect(Object component, int color, List<GuiTitlePart> parts) {
        return component != null && GeyserTextParts.collectParts(component, "minecraft:default", color, 0, parts, this.session.locale());
    }

    private SidebarDisplaySlot slot(String id) {
        for (DisplaySlot slot : this.scoreboard.getObjectiveSlots().values()) {
            if (slot instanceof SidebarDisplaySlot sidebar && slot.objectiveId().equals(id)) {
                return sidebar;
            }
        }
        return null;
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
        private final Map<String, Object> entries = new HashMap<>();
    }

    private record TeamText(Object prefix, Object suffix) {
    }
}
