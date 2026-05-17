package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.conflict.matcher.PathMatchers;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public record RetainMatchingResolution(Condition<PathContext> matcher) implements Resolution {
    public static final ResolutionFactory<RetainMatchingResolution> FACTORY = new Factory();

    @Override
    public void run(PathContext existing, PathContext conflict) {
        if (this.matcher.test(conflict)) {
            try {
                Files.copy(conflict.path(), existing.path(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                CraftEngine.instance().logger().warn("Failed to copy conflict file " + conflict + " to " + existing, e);
            }
        }
    }

    private static class Factory implements ResolutionFactory<RetainMatchingResolution> {

        @Override
        public RetainMatchingResolution create(ConfigSection section) {
            return new RetainMatchingResolution(PathMatchers.fromConfig(section.getNonNullSection("term")));
        }
    }
}
