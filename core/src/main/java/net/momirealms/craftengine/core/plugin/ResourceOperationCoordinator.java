package net.momirealms.craftengine.core.plugin;

import java.util.concurrent.atomic.AtomicReference;

/** Excludes configuration reloads and pack workflows, including work spanning multiple threads. */
public final class ResourceOperationCoordinator {
    private final AtomicReference<Lease> active = new AtomicReference<>();

    public Lease acquire() {
        Lease lease = new Lease();
        if (!this.active.compareAndSet(null, lease)) throw new BusyException();
        return lease;
    }

    public boolean isBusy() {
        return this.active.get() != null;
    }

    public final class Lease implements AutoCloseable {
        private Lease() {}

        @Override
        public void close() {
            active.compareAndSet(this, null);
        }
    }

    public static final class BusyException extends IllegalStateException {
        public BusyException() {
            super("A resource pack workflow or configuration reload is already running");
        }
    }
}
