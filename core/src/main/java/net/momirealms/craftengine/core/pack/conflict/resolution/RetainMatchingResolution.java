package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.pack.conflict.matcher.PathMatcher;
import net.momirealms.craftengine.core.pack.conflict.matcher.PathMatchers;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class RetainMatchingResolution implements Resolution {
    public static final Factory FACTORY = new Factory();
    private final PathMatcher matcher;

    public RetainMatchingResolution(PathMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public void run(Path existing, Path conflict) {
        if (this.matcher.test(existing)) {
            return;
        }
        if (this.matcher.test(conflict)) {
            try {
                Files.copy(conflict, existing, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                CraftEngine.instance().logger().warn("Failed to copy conflict file " + conflict + " to " + existing, e);
            }
        }
    }

    @Override
    public Key type() {
        return Resolutions.RETAIN_MATCHING;
    }

    public static class Factory implements ResolutionFactory {

        @Override
        public Resolution create(Map<String, Object> arguments) {
            Map<String, Object> term = MiscUtils.castToMap(arguments.get("term"), false);
            return new RetainMatchingResolution(PathMatchers.fromMap(term));
        }
    }
}
