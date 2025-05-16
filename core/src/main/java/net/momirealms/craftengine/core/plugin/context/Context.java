package net.momirealms.craftengine.core.plugin.context;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Optional;

public interface Context {

    ContextHolder contexts();

    TagResolver[] tagResolvers();

    <T> Optional<T> getOptionalParameter(ContextKey<T> parameter);

    default <T> T getParameterOrThrow(ContextKey<T> parameter) {
        return getOptionalParameter(parameter).orElseThrow(() -> new RuntimeException("No parameter found for " + parameter));
    }
}
