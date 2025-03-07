package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.Map;

public class ParentPathPrefixMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final String prefix;

    public ParentPathPrefixMatcher(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean test(Path path) {
        String pathStr = path.getParent().toString();
        return pathStr.startsWith(this.prefix);
    }

    @Override
    public Key type() {
        return PathMatchers.PARENT_PATH_PREFIX;
    }

    public static class Factory implements PathMatcherFactory {

        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            String prefix = (String) arguments.get("prefix");
            if (prefix == null) {
                throw new IllegalArgumentException("The prefix argument must not be null");
            }
            return new ParentPathPrefixMatcher(prefix);
        }
    }
}
