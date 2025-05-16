package net.momirealms.craftengine.core.plugin.context;

import java.util.Optional;

public interface ChainParameterProvider<A> {

    <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, A owner);
}
