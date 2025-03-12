package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.Map;

public class InvertedPathMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final PathMatcher matcher;

    public InvertedPathMatcher(PathMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public Key type() {
        return PathMatchers.INVERTED;
    }

    @Override
    public boolean test(Path path) {
        return !matcher.test(path);
    }

    public static class Factory implements PathMatcherFactory {

        @SuppressWarnings("unchecked")
        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            Map<String, Object> term = (Map<String, Object>) arguments.get("term");
            return new InvertedPathMatcher(PathMatchers.fromMap(term));
        }
    }
}
