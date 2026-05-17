package net.momirealms.craftengine.core.plugin.context;

import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.plugin.context.function.FunctionFactory;
import net.momirealms.craftengine.core.util.Key;

public final class CommonFunctionType<T extends Function<Context>> extends FunctionType<Context, T> {

    public CommonFunctionType(Key id, FunctionFactory<Context, T> factory) {
        super(id, factory);
    }
}
