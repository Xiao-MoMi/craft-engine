package net.momirealms.craftengine.core.plugin.context;

import java.util.Optional;

public interface AdditionalParameterProvider {

    <T> Optional<T> getOptionalParameter(ContextKey<T> parameter);
}
