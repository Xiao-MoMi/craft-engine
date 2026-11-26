package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Map;

public final class GeyserSidebarManager {
    private final ContainerTitleLayout layout;
    private final SidebarProfiles profiles;
    private final Map<String, Integer> offsets;

    public GeyserSidebarManager(ContainerTitleLayout layout, SidebarProfiles profiles, Map<String, Integer> offsets) {
        this.layout = layout;
        this.profiles = profiles;
        this.offsets = offsets;
    }

    public SidebarSession createSession(GeyserSession session) {
        return new SidebarSession(session, this.layout, this.profiles, this.offsets);
    }
}
