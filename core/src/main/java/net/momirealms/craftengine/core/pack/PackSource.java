package net.momirealms.craftengine.core.pack;

import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.ZipUtils;
import net.momirealms.sparrow.yaml.YamlDocument;
import net.momirealms.sparrow.yaml.route.Route;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public record PackSource(Path path) {

    public String name() {
        return this.path.getFileName().toString();
    }

    public boolean isArchive() {
        return Files.isRegularFile(this.path) && name().toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    public String defaultNamespace() {
        String name = name();
        return isArchive() ? name.substring(0, name.length() - 4) : name;
    }

    public Path loadArchive(FileSystem fileSystem) throws IOException {
        try (ZipFile archive = new ZipFile(this.path.toFile())) {
            ZipEntry metadata = archive.getEntry("pack.yml");
            if (metadata == null || metadata.isDirectory()) {
                throw new IOException("ZIP root must contain pack.yml");
            }
            Path folder = fileSystem.getPath("resources", name()).toAbsolutePath();
            ZipUtils.decompress(this.path, folder);
            return folder;
        }
    }

    public void setEnabled(Config config, boolean enabled) throws IOException {
        if (isArchive()) {
            try (FileSystem archive = FileSystems.newFileSystem(this.path)) {
                Path metadata = archive.getPath("/pack.yml");
                if (!Files.isRegularFile(metadata)) {
                    throw new IOException("ZIP root must contain pack.yml");
                }
                writeEnabled(config, metadata, enabled);
            }
        } else {
            Path metadata = this.path.resolve("pack.yml");
            if (Files.notExists(metadata)) {
                Files.createFile(metadata);
            }
            writeEnabled(config, metadata, enabled);
        }
    }

    private void writeEnabled(Config config, Path metadata, boolean enabled) throws IOException {
        YamlDocument document = config.loadYamlData(metadata);
        if (document == null) {
            throw new IOException("Failed to read " + metadata);
        }
        document.set(Route.from("enable"), enabled);
        document.save(metadata);
    }
}
