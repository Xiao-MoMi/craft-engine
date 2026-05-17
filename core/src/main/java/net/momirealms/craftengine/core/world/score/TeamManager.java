package net.momirealms.craftengine.core.world.score;

import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.util.LegacyChatFormatter;

import java.util.List;
import java.util.Locale;

public interface TeamManager extends Manageable {

    Object getTeamByColor(LegacyChatFormatter color);

    String getTeamNameByColor(LegacyChatFormatter color);

    static String createTeamName(LegacyChatFormatter color) {
        return "ce_" + color.name().toLowerCase(Locale.ROOT);
    }

    List<Object> addTeamsPackets();
}
