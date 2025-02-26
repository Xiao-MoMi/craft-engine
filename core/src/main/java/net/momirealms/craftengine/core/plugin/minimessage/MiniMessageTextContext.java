package net.momirealms.craftengine.core.plugin.minimessage;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.util.context.ContextHolder;

public interface MiniMessageTextContext {

    ContextHolder contexts();

    TagResolver[] tagResolvers();
}
