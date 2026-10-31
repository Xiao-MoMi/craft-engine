package net.momirealms.craftengine.bedrock.geyser.pack;

import java.util.UUID;

public record GeyserPackStatus(UUID generated, UUID served, UUID registered, State state, String reason) {

    public enum State {
        READY, RESTART_REQUIRED, WAITING_FOR_PAIR
    }

    public String message() {
        return "generated=" + version(this.generated) + ", served=" + version(this.served)
                + ", registered=" + version(this.registered) + ", status=" + this.state
                + (this.reason.isEmpty() ? "" : "; " + this.reason);
    }

    private static String version(UUID uuid) {
        return uuid == null ? "unavailable" : uuid.toString();
    }
}
