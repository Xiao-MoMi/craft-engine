package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

public interface ConfigSectionParser extends Comparable<ConfigSectionParser> {

    String sectionId();

    void parseSection(Pack pack, Path path, Key id, Map<String, Object> section);

    int loadingSequence();

    @Override
    default int compareTo(@NotNull ConfigSectionParser another) {
        return Integer.compare(loadingSequence(), another.loadingSequence());
    }
}
