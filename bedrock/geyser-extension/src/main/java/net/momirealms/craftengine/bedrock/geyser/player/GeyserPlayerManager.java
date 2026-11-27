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
    private final Map<GeyserConnection, GeyserPlayer> players = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public GeyserPlayerManager(CraftEngineGeyserExtension extension) {
        this.extension = extension;
    }

    public void add(GeyserSession session) {
        if (this.closed || session.isClosed() || this.players.containsKey(session)) {
            return;
        }
        GeyserPlayer player = new GeyserPlayer(this.extension, session);
        synchronized (player) {
            synchronized (this) {
                if (this.closed || session.isClosed() || this.players.putIfAbsent(session, player) != null) {
                    return;
                }
            }
            try {
                player.initialize();
            } catch (Throwable e) {
                this.players.remove(session, player);
                player.close();
                throw e;
            }
        }
    }

    @Nullable
    public GeyserPlayer get(GeyserConnection connection) {
        return this.players.get(connection);
    }

    public void remove(GeyserConnection connection) {
        GeyserPlayer player = this.players.remove(connection);
        if (player != null) {
            player.close();
        }
    }

    public void activateJavaListeners() {
        for (GeyserPlayer player : this.players.values()) {
            player.javaConnection().activate();
        }
    }

    public void disable() {
        List<GeyserPlayer> players;
        synchronized (this) {
            this.closed = true;
            players = List.copyOf(this.players.values());
            this.players.clear();
        }
        for (GeyserPlayer player : players) {
            player.close();
        }
    }
}
