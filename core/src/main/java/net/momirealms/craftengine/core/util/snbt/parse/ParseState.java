package net.momirealms.craftengine.core.util.snbt.parse;

import javax.annotation.Nullable;
import java.util.Optional;

public interface ParseState<S> {
    Scope scope();

    ErrorCollector<S> errorCollector();

    default <T> Optional<T> parseTopRule(NamedRule<S, T> rule) {
        T object = this.parse(rule);
        if (object != null) {
            this.errorCollector().finish(this.mark());
        }

        if (!this.scope().hasOnlySingleFrame()) {
            throw new IllegalStateException("Malformed scope: " + this.scope());
        } else {
            return Optional.ofNullable(object);
        }
    }

    @Nullable
    <T> T parse(NamedRule<S, T> rule);

    S input();

    int mark();

    void restore(int cursor);

    Control acquireControl();

    void releaseControl();

    ParseState<S> silent();

    void markNull(int mark);

    boolean isNull(int mark);
}
