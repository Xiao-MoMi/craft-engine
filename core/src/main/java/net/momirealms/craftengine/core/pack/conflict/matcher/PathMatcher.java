package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.util.Key;

import java.nio.file.Path;
import java.util.function.Predicate;

public interface PathMatcher extends Predicate<Path> {

    Key type();
}
