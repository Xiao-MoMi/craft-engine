package net.momirealms.craftengine.bedrock.geyser.player;

import net.momirealms.craftengine.bedrock.geyser.CraftEngineGeyserExtension;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GeyserPlayerManager {
    private final CraftEngineGeyserExtension extension;
    private final Map<GeyserSession, GeyserPlayer> players = new ConcurrentHashMap<>();
    private boolean closed;

    public GeyserPlayerManager(CraftEngineGeyserExtension extension) {
        this.extension = extension;
    }

    public void add(GeyserSession session) {
        GeyserPlayer player;
        synchronized (this) {
            if (this.closed || session.isClosed() || this.players.containsKey(session)) {
                return;
            }
            player = new GeyserPlayer(this.extension, session);
            this.players.put(session, player);
        }
        try {
            player.initialize();
        } catch (RuntimeException | Error e) {
            this.players.remove(session, player);
            player.close();
            throw e;
        }
    }

    @Nullable
    public GeyserPlayer get(GeyserConnection connection) {
        GeyserPlayer player = this.players.get(connection);
        return player == null || player.isClosed() ? null : player;
    }

    public void remove(GeyserConnection connection) {
        GeyserPlayer player = this.players.remove(connection);
        if (player != null) {
            player.close();
        }
    }

    public void disable() {
        List<GeyserPlayer> players;
        synchronized (this) {
            this.closed = true;
            players = List.copyOf(this.players.values());
            this.players.clear();
        }
        // Sidebar cleanup takes its own lock; never hold the registry lock while closing a player.
        for (GeyserPlayer player : players) {
            player.close();
        }
    }
}
