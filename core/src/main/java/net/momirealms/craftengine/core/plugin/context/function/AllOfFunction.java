package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.context.Context;

import java.util.Collection;

public final class AllOfFunction<CTX extends Context> implements Function<CTX> {
    private final Function<CTX>[] functions;

    private AllOfFunction(Function<CTX>[] functions) {
        this.functions = functions;
    }

    @SuppressWarnings("unchecked")
    private AllOfFunction(Collection<Function<CTX>> functions) {
        this.functions = functions.toArray(new Function[0]);
    }

    @SafeVarargs
    public static <CTX extends Context> AllOfFunction<CTX> allOf(Function<CTX>... functions) {
        return new AllOfFunction<>(functions);
    }

    public static <CTX extends Context> AllOfFunction<CTX> allOf(Collection<Function<CTX>> functions) {
        return new AllOfFunction<>(functions);
    }

    @Override
    public void run(CTX ctx) {
        for (Function<CTX> function : this.functions) {
            function.run(ctx);
        }
    }
}
