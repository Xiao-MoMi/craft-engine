package net.momirealms.craftengine.core.plugin.context;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.text.minimessage.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public abstract class AbstractCommonContext implements Context {
    protected final ContextHolder contexts;
    protected final List<AdditionalParameterProvider> additionalParameterProviders;
    protected TagResolver[] tagResolvers;

    public AbstractCommonContext(ContextHolder contexts) {
        this.contexts = contexts;
        this.additionalParameterProviders = List.of(new CommonParameterProvider());
    }

    public AbstractCommonContext(ContextHolder contexts,
                                 List<AdditionalParameterProvider> additionalParameterProviders) {
        this.contexts = contexts;
        this.additionalParameterProviders = additionalParameterProviders;
    }

    @Override
    public ContextHolder contexts() {
        return this.contexts;
    }

    @Override
    @NotNull
    public TagResolver[] tagResolvers() {
        if (this.tagResolvers == null) {
            this.tagResolvers = new TagResolver[]{ShiftTag.INSTANCE, ImageTag.INSTANCE, new I18NTag(this), new NamedArgumentTag(this),
                    new PlaceholderTag(null), new ExpressionTag(this), new GlobalVariableTag(this)};
        }
        return this.tagResolvers;
    }

    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
        if (!this.additionalParameterProviders.isEmpty()) {
            for (AdditionalParameterProvider additionalParameterProvider : additionalParameterProviders) {
                Optional<T> optionalValue = additionalParameterProvider.getOptionalParameter(parameter);
                if (optionalValue.isPresent()) {
                    return optionalValue;
                }
            }
        }
        return this.contexts.getOptional(parameter);
    }
}
