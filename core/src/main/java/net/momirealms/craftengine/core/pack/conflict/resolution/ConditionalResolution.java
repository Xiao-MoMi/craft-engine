package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.pack.conflict.matcher.PathMatchers;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.Condition;

public record ConditionalResolution(Condition<PathContext> matcher, Resolution resolution) implements Resolution {
    public static final ResolutionFactory<ConditionalResolution> FACTORY = new Factory();

    @Override
    public void run(PathContext existing, PathContext conflict) {
        if (this.matcher.test(existing)) {
            this.resolution.run(existing, conflict);
        }
    }

    private static class Factory implements ResolutionFactory<ConditionalResolution> {

        @Override
        public ConditionalResolution create(ConfigSection section) {
            return new ConditionalResolution(
                    PathMatchers.fromConfig(section.getNonNullSection("term")),
                    Resolutions.fromConfig(section.getNonNullSection("resolution"))
            );
        }
    }
}
