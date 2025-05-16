package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

public interface ConfigParser extends Comparable<ConfigParser> {

    String[] sectionId();

    default void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) throws LocalizedException {
        this.parseObject(pack, path, id, section);
    }

    default void parseObject(Pack pack, Path path, Key id, Object object) throws LocalizedException {
    }

    int loadingSequence();

    default boolean supportsParsingObject() {
        return false;
    }

    @Override
    default int compareTo(@NotNull ConfigParser another) {
        return Integer.compare(loadingSequence(), another.loadingSequence());
    }
}
