package net.momirealms.craftengine.core.pack;

import java.nio.file.Path;

public record PackZipRequest(Path source, Path output, boolean protection) {
}
