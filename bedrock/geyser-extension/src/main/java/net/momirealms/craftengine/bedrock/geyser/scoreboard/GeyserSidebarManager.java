package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.geyser.session.GeyserSession;

public final class GeyserSidebarManager {
    private final ContainerTitleLayout layout;
    private final SidebarProfiles profiles;

    public GeyserSidebarManager(ContainerTitleLayout layout, SidebarProfiles profiles) {
        this.layout = layout;
        this.profiles = profiles;
    }

    public GeyserSidebarConnection createConnection(GeyserSession session) {
        return new GeyserSidebarConnection(session, this.layout, this.profiles);
    }
}
