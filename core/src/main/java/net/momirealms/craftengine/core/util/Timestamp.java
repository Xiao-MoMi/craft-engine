package net.momirealms.craftengine.core.util;

public final class Timestamp {
    private long lastMillis;

    public Timestamp() {
        this.lastMillis = System.currentTimeMillis();
    }

    public long deltaMillis() {
        long now = System.currentTimeMillis();
        long deltaMillis = now - lastMillis;
        lastMillis = now;
        return deltaMillis;
    }
}
