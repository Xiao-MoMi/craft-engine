package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.plugin.context.Context;

public final class DummyFunction<CTX extends Context> implements Function<CTX> {
    public static final DummyFunction<Context> INSTANCE = new DummyFunction<>();

    private DummyFunction() {}

    @Override
    public void run(CTX ctx) {
    }
}
