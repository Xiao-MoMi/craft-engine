package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ExactPathMatcher implements PathMatcher {
    public static final Factory FACTORY = new Factory();
    private final List<String> path;

    public ExactPathMatcher(List<String> path) {
        this.path = path;
    }

    @Override
    public boolean test(Path path) {
        String pathStr = path.getParent().toString();
        for (String p : this.path) {
            if (pathStr.equals(p)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Key type() {
        return PathMatchers.EXACT;
    }

    public static class Factory implements PathMatcherFactory {

        @Override
        public PathMatcher create(Map<String, Object> arguments) {
            List<String> path = MiscUtils.getAsStringList(arguments.get("path"));
            return new ExactPathMatcher(path);
        }
    }
}
