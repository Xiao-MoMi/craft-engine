package net.momirealms.craftengine.core.item;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.minimessage.*;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.minimessage.MiniMessageTextContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemBuildContext implements MiniMessageTextContext {
    public static final ItemBuildContext EMPTY = new ItemBuildContext(null, ContextHolder.EMPTY);
    private final Player player;
    private final ContextHolder contexts;
    private TagResolver[] tagResolvers;

    public ItemBuildContext(@Nullable Player player, @NotNull ContextHolder contexts) {
        this.player = player;
        this.contexts = contexts;
    }

    @NotNull
    public static ItemBuildContext of(@Nullable Player player, @NotNull ContextHolder contexts) {
        return new ItemBuildContext(player, contexts);
    }

    @Nullable
    public Player player() {
        return this.player;
    }

    @NotNull
    public ContextHolder contexts() {
        return this.contexts;
    }

    @NotNull
    public TagResolver[] tagResolvers() {
        if (this.tagResolvers == null) {
            this.tagResolvers = new TagResolver[]{ShiftTag.INSTANCE, ImageTag.INSTANCE, new PlaceholderTag(this.player), new I18NTag(this), new NamedArgumentTag(this)};
        }
        return this.tagResolvers;
    }
}
