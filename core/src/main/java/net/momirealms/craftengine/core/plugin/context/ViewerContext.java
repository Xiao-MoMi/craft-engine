package net.momirealms.craftengine.core.plugin.context;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.text.minimessage.*;

import java.util.Optional;

public class ViewerContext implements RelationalContext {
    private final Context owner;
    private final PlayerOptionalContext viewer;
    private TagResolver[] tagResolvers;

    public ViewerContext(Context owner, PlayerOptionalContext viewer) {
        this.owner = owner;
        this.viewer = viewer;
    }

    public static ViewerContext of(Context owner, PlayerOptionalContext viewer) {
        return new ViewerContext(owner, viewer);
    }

    @Override
    public <T> Optional<T> getViewerOptionalParameter(ContextKey<T> parameter) {
        return this.viewer.getOptionalParameter(parameter);
    }

    @Override
    public ContextHolder viewerContexts() {
        return this.viewer.contexts();
    }

    @Override
    public <T> T getViewerParameterOrThrow(ContextKey<T> parameter) {
        return this.viewer.getParameterOrThrow(parameter);
    }

    @Override
    public ContextHolder contexts() {
        return this.owner.contexts();
    }

    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter) {
        return this.owner.getOptionalParameter(parameter);
    }

    @Override
    public <T> T getParameterOrThrow(ContextKey<T> parameter) {
        return this.owner.getParameterOrThrow(parameter);
    }

    @Override
    public TagResolver[] tagResolvers() {
        if (this.tagResolvers == null) {
            if (this.owner instanceof PlayerOptionalContext context && context.player != null && this.viewer.player != null) {
                this.tagResolvers = new TagResolver[]{new RelationalPlaceholderTag(context.player, this.viewer.player, this),
                        ShiftTag.INSTANCE, ImageTag.INSTANCE,
                        new PlaceholderTag(this.owner), new ViewerPlaceholderTag(this.viewer),
                        new NamedArgumentTag(this.owner), new ViewerNamedArgumentTag(this.viewer),
                        I18NTag.INSTANCE, PlainL10NTag.INSTANCE, ExpressionTag.INSTANCE, GlobalVariableTag.INSTANCE};
            } else {
                this.tagResolvers = new TagResolver[]{ShiftTag.INSTANCE, ImageTag.INSTANCE,
                        new PlaceholderTag(this.owner), new ViewerPlaceholderTag(this.viewer),
                        new NamedArgumentTag(this.owner), new ViewerNamedArgumentTag(this.viewer),
                        I18NTag.INSTANCE, PlainL10NTag.INSTANCE, ExpressionTag.INSTANCE, GlobalVariableTag.INSTANCE};
            }
        }
        return this.tagResolvers;
    }
}
