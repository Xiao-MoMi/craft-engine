package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;

public interface Resolution {

    void run(Path existing, Path conflict);

    Key type();
}
