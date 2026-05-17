package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.util.CharacterUtils;

import java.nio.file.Path;

public record ParentSuffixPathMatcher(String suffix) implements Condition<PathContext> {
    public static final ConditionFactory<PathContext, ParentSuffixPathMatcher> FACTORY = new Factory();

    @Override
    public boolean test(PathContext context) {
        Path parent = context.path().getParent();
        if (parent == null) return false;
        String pathStr = CharacterUtils.replaceBackslashWithSlash(parent.toString());
        return pathStr.endsWith(suffix);
    }

    private static class Factory implements ConditionFactory<PathContext, ParentSuffixPathMatcher> {
        @Override
        public ParentSuffixPathMatcher create(ConfigSection section) {
            return new ParentSuffixPathMatcher(section.getNonNullString("suffix"));
        }
    }
}