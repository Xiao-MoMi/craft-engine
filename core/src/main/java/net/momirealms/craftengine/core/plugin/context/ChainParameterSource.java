package net.momirealms.craftengine.core.plugin.context;

import java.util.Optional;

public interface ChainParameterSource {

    <T> Optional<T> getParameter(ContextKey<T> key);
}
