package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.util.CharacterUtils;

public record ExactPathMatcher(String path) implements Condition<PathContext> {
    public static final ConditionFactory<PathContext, ExactPathMatcher> FACTORY = new Factory();

    @Override
    public boolean test(PathContext context) {
        String pathStr = CharacterUtils.replaceBackslashWithSlash(context.path().toString());
        return pathStr.equals(this.path);
    }

    private static class Factory implements ConditionFactory<PathContext, ExactPathMatcher> {
        @Override
        public ExactPathMatcher create(ConfigSection section) {
            return new ExactPathMatcher(section.getNonNullString("path"));
        }
    }
}