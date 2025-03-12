package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class AnyOfPathMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final List<PathMatcher> matchers;

    public AnyOfPathMatcher(List<PathMatcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    public Key type() {
        return PathMatchers.ANY_OF;
    }

    @Override
    public boolean test(Path path) {
        for (PathMatcher matcher : matchers) {
            if (matcher.test(path)) {
                return true;
            }
        }
        return false;
    }

    public static class Factory implements PathMatcherFactory {

        @SuppressWarnings("unchecked")
        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            List<Map<String, Object>> terms = (List<Map<String, Object>>) arguments.get("terms");
            return new AnyOfPathMatcher(PathMatchers.fromMapList(terms));
        }
    }
}
