package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;

public record FilenamePathMatcher(String name) implements Condition<PathContext> {
    public static final ConditionFactory<PathContext, FilenamePathMatcher> FACTORY = new Factory();

    @Override
    public boolean test(PathContext context) {
        String fileName = String.valueOf(context.path().getFileName());
        return fileName.equals(name);
    }

    private static class Factory implements ConditionFactory<PathContext, FilenamePathMatcher> {
        @Override
        public FilenamePathMatcher create(ConfigSection section) {
            return new FilenamePathMatcher(section.getNonNullString("name"));
        }
    }
}