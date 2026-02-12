package net.momirealms.craftengine.core.plugin.context;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.text.minimessage.*;
import net.momirealms.craftengine.core.util.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            this.tagResolvers = getTagResolver();
        }
        return this.tagResolvers;
    }

    @Nullable
    protected TagResolver[] getTagResolver() {
        return ArrayUtils.mergeNoCopy(getExternalTagResolvers(), getInternalTagResolvers());
    }

    @Nullable
    protected TagResolver[] getExternalTagResolvers() {
        return CraftEngine.instance().compatibilityManager().createExternalTagResolvers(this);
    }

    @NotNull
    protected TagResolver[] getInternalTagResolvers() {
        return new TagResolver[]{ShiftTag.INSTANCE, ImageTag.INSTANCE, I18NTag.INSTANCE, PlainL10NTag.INSTANCE, new NamedArgumentTag(this),
                new PlaceholderTag(this), ExpressionTag.INSTANCE, GlobalVariableTag.INSTANCE};
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
