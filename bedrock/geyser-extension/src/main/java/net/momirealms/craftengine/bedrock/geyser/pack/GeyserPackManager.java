package net.momirealms.craftengine.bedrock.geyser.pack;

import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserGlyphs;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserTextData;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.pack.PathPackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class GeyserPackManager {
    private final Path packDirectory;
    private final Path dataDirectory;
    private final Path snapshotDirectory;
    private final Map<Path, GeyserPackSnapshot> packs = new LinkedHashMap<>();
    private final List<Path> registrationFiles;
    private final GeyserTextData text;

    public GeyserPackManager(Path packDirectory, Path dataDirectory, Path snapshotDirectory) throws IOException {
        this.packDirectory = packDirectory;
        this.dataDirectory = dataDirectory;
        this.snapshotDirectory = snapshotDirectory;
        this.removePreviousSnapshots();
        for (Path path : this.sources()) {
            this.packs.put(path, GeyserPackSnapshot.read(path, BedrockPackFiles.data(path, this.dataDirectory), this.snapshotDirectory));
        }
        this.registrationFiles = this.packs.values().stream().map(GeyserPackSnapshot::data).toList();
        this.text = GeyserGlyphs.read(this.registrationFiles);
    }

    public void prepare() {
        // ResourcePack providers initialize Geyser registries. Only call them after custom registration events finish.
        this.packs.values().forEach(GeyserPackSnapshot::prepare);
    }

    public List<Path> registrationFiles() {
        return this.registrationFiles;
    }

    public GeyserTextData text() {
        return this.text;
    }

    public void load(SessionLoadResourcePacksEvent event) {
        for (Map.Entry<Path, GeyserPackSnapshot> entry : this.packs.entrySet()) {
            ResourcePackOption<?>[] options = new ResourcePackOption<?>[0];
            for (ResourcePack offered : List.copyOf(event.resourcePacks())) {
                if (entry.getValue().uuid().equals(offered.uuid()) || (offered.codec() instanceof PathPackCodec file && file.path().toAbsolutePath().normalize().equals(entry.getKey().toAbsolutePath().normalize()))) {
                    options = event.options(offered.uuid()).toArray(ResourcePackOption<?>[]::new);
                    event.unregister(offered.uuid());
                }
            }
            event.register(entry.getValue().pack(), options);
        }
    }

    private List<Path> sources() throws IOException {
        if (!Files.isDirectory(this.packDirectory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(this.packDirectory)) {
            return paths.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return (name.endsWith(".mcpack") || name.endsWith(".zip")) && Files.isRegularFile(BedrockPackFiles.data(path, this.dataDirectory));
            }).sorted().toList();
        }
    }

    private void removePreviousSnapshots() throws IOException {
        if (!Files.isDirectory(this.snapshotDirectory)) {
            return;
        }
        try (Stream<Path> paths = Files.list(this.snapshotDirectory)) {
            for (Path path : paths.filter(Files::isDirectory).filter(path -> path.getFileName().toString().startsWith("pack-")).toList()) {
                Files.deleteIfExists(path.resolve("pack.mcpack"));
                Files.deleteIfExists(path.resolve("pack.ce-data"));
                Files.delete(path);
            }
        }
    }
}
