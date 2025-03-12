package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.Map;

public class PathPatternMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final String pattern;

    public PathPatternMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean test(Path path) {
        String pathStr = path.toString().replace("\\", "/");
        return pathStr.matches(pattern);
    }

    @Override
    public Key type() {
        return PathMatchers.PATTERN;
    }

    public static class Factory implements PathMatcherFactory {

        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            String pattern = (String) arguments.get("pattern");
            if (pattern == null) {
                throw new IllegalArgumentException("The pattern argument must not be null");
            }
            return new PathPatternMatcher(pattern);
        }
    }
}
