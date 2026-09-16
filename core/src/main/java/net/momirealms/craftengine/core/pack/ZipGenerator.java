package net.momirealms.craftengine.core.pack;

import java.io.IOException;

@FunctionalInterface
public interface ZipGenerator {

    void generate(PackZipRequest request) throws IOException;
}
