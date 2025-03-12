package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.Map;

public class ParentPathSuffixMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final String suffix;

    public ParentPathSuffixMatcher(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public boolean test(Path path) {
        Path parent = path.getParent();
        if (parent == null) return false;
        String pathStr = parent.toString().replace("\\", "/");
        return pathStr.endsWith(suffix);
    }

    @Override
    public Key type() {
        return PathMatchers.PARENT_PATH_SUFFIX;
    }

    public static class Factory implements PathMatcherFactory {

        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            String suffix = (String) arguments.get("suffix");
            if (suffix == null) {
                throw new IllegalArgumentException("The suffix argument must not be null");
            }
            return new ParentPathSuffixMatcher(suffix);
        }
    }
}
