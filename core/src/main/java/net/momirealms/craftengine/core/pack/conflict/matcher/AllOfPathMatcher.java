package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class AllOfPathMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final List<PathMatcher> matchers;

    public AllOfPathMatcher(List<PathMatcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    public Key type() {
        return PathMatchers.ALL_OF;
    }

    @Override
    public boolean test(Path path) {
        for (PathMatcher matcher : matchers) {
            if (!matcher.test(path)) {
                return false;
            }
        }
        return true;
    }

    public static class Factory implements PathMatcherFactory {

        @SuppressWarnings("unchecked")
        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            List<Map<String, Object>> terms = (List<Map<String, Object>>) arguments.get("terms");
            return new AllOfPathMatcher(PathMatchers.fromMapList(terms));
        }
    }
}
