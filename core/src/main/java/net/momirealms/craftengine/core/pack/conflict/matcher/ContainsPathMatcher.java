package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.util.CharacterUtils;

public record ContainsPathMatcher(String path) implements Condition<PathContext> {
    public static final ConditionFactory<PathContext, ContainsPathMatcher> FACTORY = new Factory();

    @Override
    public boolean test(PathContext path) {
        String pathStr = CharacterUtils.replaceBackslashWithSlash(path.path().toString());
        return pathStr.contains(this.path);
    }

    private static class Factory implements ConditionFactory<PathContext, ContainsPathMatcher> {
        @Override
        public ContainsPathMatcher create(ConfigSection section) {
            return new ContainsPathMatcher(section.getNonNullString("path"));
        }
    }
}