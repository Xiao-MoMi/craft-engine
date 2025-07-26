package net.momirealms.craftengine.core.plugin.context;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.text.minimessage.*;

import java.util.Optional;

public class ViewerContext implements RelationalContext {
    private final Context owner;
    private final PlayerOptionalContext viewer;
    private final List<TagResolver> tagResolvers = new ArrayList<>();

    public ViewerContext(Context owner, PlayerOptionalContext viewer) {
        this.owner = owner;
        this.viewer = viewer;
            if (this.owner instanceof PlayerOptionalContext context && context.player != null && this.viewer.player != null) {
                this.tagResolvers.addAll(List.of(new RelationalPlaceholderTag(context.player, this.viewer.player, this),
                        ShiftTag.INSTANCE, ImageTag.INSTANCE,
                        new PlaceholderTag(this.owner), new ViewerPlaceholderTag(this.viewer),
                        new NamedArgumentTag(this.owner), new ViewerNamedArgumentTag(this.viewer),
                        new I18NTag(this), new ExpressionTag(this), new GlobalVariableTag(this)));
            } else {
                this.tagResolvers.addAll(List.of(ShiftTag.INSTANCE, ImageTag.INSTANCE,
                        new PlaceholderTag(this.owner), new ViewerPlaceholderTag(this.viewer),
                        new NamedArgumentTag(this.owner), new ViewerNamedArgumentTag(this.viewer),
                        new I18NTag(this), new ExpressionTag(this), new GlobalVariableTag(this)));
            }
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
    public List<TagResolver> tagResolvers() {
        return this.tagResolvers;
    }
}
