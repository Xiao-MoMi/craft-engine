package net.momirealms.craftengine.core.plugin.config.lifecycle;

import java.util.concurrent.atomic.AtomicInteger;

public final class LoadingStage {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private final int id;
    private final String name;

    public LoadingStage(String name) {
        this.id = COUNTER.getAndIncrement();
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public int id() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LoadingStage that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.id);
    }

    @Override
    public String toString() {
        return "LoadingStage{" +
                "name='" + this.name + '\'' +
                '}';
    }
}
