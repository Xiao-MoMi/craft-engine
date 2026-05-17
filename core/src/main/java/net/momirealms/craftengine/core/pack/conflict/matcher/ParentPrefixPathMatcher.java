package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.util.CharacterUtils;

import java.nio.file.Path;

public record ParentPrefixPathMatcher(String prefix) implements Condition<PathContext> {
    public static final ConditionFactory<PathContext, ParentPrefixPathMatcher> FACTORY = new Factory();

    @Override
    public boolean test(PathContext context) {
        Path parent = context.path().getParent();
        if (parent == null) return false;
        String pathStr = CharacterUtils.replaceBackslashWithSlash(parent.toString());
        return pathStr.startsWith(this.prefix);
    }

    private static class Factory implements ConditionFactory<PathContext, ParentPrefixPathMatcher> {
        @Override
        public ParentPrefixPathMatcher create(ConfigSection section) {
            return new ParentPrefixPathMatcher(section.getNonNullString("prefix"));
        }
    }
}