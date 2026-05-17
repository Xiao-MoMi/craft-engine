package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.context.Context;

import java.util.List;

public interface Function<CTX extends Context> {

    void run(CTX ctx);

    @SuppressWarnings("unchecked")
    static <CTX extends Context> Function<CTX> allOf(List<Function<CTX>> functions) {
        if (functions == null || functions.isEmpty()) {
            return (Function<CTX>) DummyFunction.INSTANCE;
        }
        if (functions.size() == 1) {
            return functions.getFirst();
        }
        return AllOfFunction.allOf(functions);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    static <CTX extends Context> Function<CTX> allOf(Function<CTX>... functions) {
        if (functions == null || functions.length == 0) {
            return (Function<CTX>) DummyFunction.INSTANCE;
        }
        if (functions.length == 1) {
            return functions[0];
        }
        return AllOfFunction.allOf(functions);
    }
}
