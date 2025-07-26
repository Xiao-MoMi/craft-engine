package net.momirealms.craftengine.core.plugin.context;

import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Optional;

public interface Context {

    ContextHolder contexts();

    List<TagResolver> tagResolvers();

    default TagResolver combinedTagResolver() {
        return TagResolver.resolver(tagResolvers());
    }

    <T> Optional<T> getOptionalParameter(ContextKey<T> parameter);

    default <T> T getParameterOrThrow(ContextKey<T> parameter) {
        return getOptionalParameter(parameter).orElseThrow(() -> new RuntimeException("No parameter found for " + parameter));
    }
}
