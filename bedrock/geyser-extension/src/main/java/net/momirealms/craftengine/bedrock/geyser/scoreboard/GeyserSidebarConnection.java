package net.momirealms.craftengine.bedrock.geyser.scoreboard;

import net.momirealms.craftengine.bedrock.geyser.proxy.session.GeyserSessionProxy;
import net.momirealms.craftengine.bedrock.text.ContainerTitleLayout;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.UpstreamSession;

public final class GeyserSidebarConnection {
    private final GeyserSession session;
    private final SidebarSession sidebar;
    private final UpstreamSession original;
    private final UpstreamSession wrapped;

    public GeyserSidebarConnection(GeyserSession session, ContainerTitleLayout layout, SidebarProfiles profiles) {
        this.session = session;
        this.sidebar = new SidebarSession(session, layout, profiles);
        this.original = session.getUpstream();
        this.wrapped = new SidebarUpstream(this.original, this.sidebar).wrap();
        GeyserSessionProxy.INSTANCE.upstream(session, this.wrapped);
    }

    public SidebarSession sidebar() {
        return this.sidebar;
    }

    public void close() {
        synchronized (this.sidebar) {
            if (this.session.getUpstream() == this.wrapped) {
                GeyserSessionProxy.INSTANCE.upstream(this.session, this.original);
            }
        }
    }
}
