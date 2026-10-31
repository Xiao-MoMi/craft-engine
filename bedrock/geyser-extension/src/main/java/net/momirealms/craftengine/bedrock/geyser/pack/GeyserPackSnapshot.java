package net.momirealms.craftengine.bedrock.geyser.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.GeyserRegistrationPlanReader;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipFile;

public final class GeyserPackSnapshot {
    private final Path directory;
    private final Path data;
    private final UUID uuid;
    private ResourcePack pack;
    private final List<JsonObject> registrations;

    private GeyserPackSnapshot(Path directory, Path data, UUID uuid, List<JsonObject> registrations) {
        this.directory = directory;
        this.data = data;
        this.uuid = uuid;
        this.registrations = registrations;
    }

    public static GeyserPackSnapshot read(Path source, Path dataSource, Path store) throws IOException {
        Files.createDirectories(store);
        Path directory = Files.createTempDirectory(store, "pack-");
        Path pack = directory.resolve("pack.mcpack");
        Path data = directory.resolve("pack.ce-data");
        try {
            Files.copy(source, pack, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(dataSource, data, StandardCopyOption.REPLACE_EXISTING);
            JsonObject header = header(pack);
            if (!header.equals(header(data))) {
                throw new IOException("Resource pack and extension data belong to different builds: " + source);
            }
            List<JsonObject> registrations = List.of(
                    GeyserRegistrationPlanReader.readPack(data, RegistrationPlanKind.ITEM),
                    GeyserRegistrationPlanReader.readPack(data, RegistrationPlanKind.BLOCK));
            return new GeyserPackSnapshot(directory, data, UUID.fromString(header.get("uuid").getAsString()), registrations);
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(pack);
            Files.deleteIfExists(data);
            Files.deleteIfExists(directory);
            throw exception;
        }
    }

    private static JsonObject header(Path path) throws IOException {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            if (zip.getEntry("manifest.json") == null) {
                throw new IOException("Missing paired pack manifest: " + path);
            }
            try (Reader reader = new InputStreamReader(zip.getInputStream(zip.getEntry("manifest.json")), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("header");
            }
        }
    }

    public Path data() {
        return this.data;
    }

    public ResourcePack pack() {
        return this.pack;
    }

    public void prepare() {
        if (this.pack == null) {
            ResourcePack resource = ResourcePack.create(PackCodec.path(this.directory.resolve("pack.mcpack")));
            resource.codec().sha256();
            resource.codec().size();
            this.pack = resource;
        }
    }

    public UUID uuid() {
        return this.uuid;
    }

    public boolean sameRegistrations(GeyserPackSnapshot other) {
        return this.registrations.equals(other.registrations);
    }

    public void delete() throws IOException {
        Files.deleteIfExists(this.directory.resolve("pack.mcpack"));
        Files.deleteIfExists(this.data);
        Files.deleteIfExists(this.directory);
    }
}
