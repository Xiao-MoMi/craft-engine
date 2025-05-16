package net.momirealms.craftengine.core.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancel);

    static Cancellable dummy() {
        return new Dummy();
    }

    static Cancellable of(Supplier<Boolean> isCancelled, Consumer<Boolean> setCancelled) {
        return new Simple(isCancelled, setCancelled);
    }

    class Dummy implements Cancellable {
        private boolean cancelled;

        @Override
        public boolean isCancelled() {
            return this.cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }
    }

    class Simple implements Cancellable {
        private final Supplier<Boolean> isCancelled;
        private final Consumer<Boolean> setCancelled;

        public Simple(Supplier<Boolean> isCancelled, Consumer<Boolean> setCancelled) {
            this.isCancelled = isCancelled;
            this.setCancelled = setCancelled;
        }

        @Override
        public boolean isCancelled() {
            return this.isCancelled.get();
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.setCancelled.accept(cancel);
        }
    }
}
