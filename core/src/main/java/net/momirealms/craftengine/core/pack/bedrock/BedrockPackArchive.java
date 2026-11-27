package net.momirealms.craftengine.core.pack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.core.util.FileUtils;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ZipUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;

final class BedrockPackArchive {
    private BedrockPackArchive() {
    }

    static void write(Path pack, Path data, Path dataDestination, boolean protection, String name, String description) throws IOException {
        JsonObject manifest = manifest(name, description);
        String fingerprint = protection ? null : contentFingerprint(pack, manifest);
        Path dataDirectory = Files.createDirectories(dataDestination.getParent());
        Path temporaryData = Files.createTempFile(dataDirectory, "bedrock-data-", ".tmp");
        try {
            if (protection) {
                Files.deleteIfExists(data.resolve(BedrockPackFiles.CONTENT_SHA256));
            } else {
                Files.writeString(data.resolve(BedrockPackFiles.CONTENT_SHA256), fingerprint);
            }
            if (protection || !reuseClientPack(data, dataDestination, temporaryData, fingerprint)) {
                UUID header = protection ? UUID.randomUUID() : UUID.nameUUIDFromBytes(("header/" + fingerprint).getBytes(StandardCharsets.US_ASCII));
                UUID module = protection ? UUID.randomUUID() : UUID.nameUUIDFromBytes(("module/" + fingerprint).getBytes(StandardCharsets.US_ASCII));
                manifest.getAsJsonObject("header").addProperty("uuid", header.toString());
                manifest.getAsJsonArray("modules").get(0).getAsJsonObject().addProperty("uuid", module.toString());
                GsonHelper.writeJsonFile(manifest, pack.resolve("manifest.json"));
                writeClientPack(pack, data, temporaryData);
            }
            Files.move(temporaryData, dataDestination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryData);
        }
    }

    private static String contentFingerprint(Path pack, JsonObject manifest) throws IOException {
        Map<String, Path> entries = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(pack)) {
            paths.filter(Files::isRegularFile).forEach(path -> entries.put(pack.relativize(path).toString().replace('\\', '/'), path));
        }
        entries.remove("manifest.json");
        MessageDigest digest = BedrockPackFiles.sha256();
        try (DataOutputStream output = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), digest))) {
            byte[] metadata = manifest.toString().getBytes(StandardCharsets.UTF_8);
            output.writeInt(metadata.length);
            output.write(metadata);
            byte[] buffer = new byte[16 * 1024];
            for (Map.Entry<String, Path> entry : entries.entrySet()) {
                output.writeUTF(entry.getKey());
                output.writeLong(Files.size(entry.getValue()));
                try (InputStream input = Files.newInputStream(entry.getValue())) {
                    FileUtils.copy(input, output, buffer);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static boolean reuseClientPack(Path data, Path previous, Path output, String fingerprint) {
        if (!Files.isRegularFile(previous)) {
            return false;
        }
        try (ZipFile archive = new ZipFile(previous.toFile())) {
            ZipEntry client = archive.getEntry(BedrockPackFiles.CLIENT_PACK);
            if (client == null || archive.getEntry(BedrockPackFiles.CONTENT_SHA256) == null
                    || !fingerprint.equals(BedrockPackFiles.readDigest(archive, BedrockPackFiles.CONTENT_SHA256))) {
                return false;
            }
            String expected = BedrockPackFiles.readDigest(archive, BedrockPackFiles.CLIENT_SHA256);
            Files.writeString(data.resolve(BedrockPackFiles.CLIENT_SHA256), expected);
            MessageDigest digest = BedrockPackFiles.sha256();
            try (InputStream input = new DigestInputStream(archive.getInputStream(client), digest)) {
                ZipUtils.compressWithStoredEntry(data, output, BedrockPackFiles.CLIENT_PACK, input, client.getSize(), client.getCrc());
            }
            return expected.equals(HexFormat.of().formatHex(digest.digest()));
        } catch (IOException exception) {
            return false;
        }
    }

    private static void writeClientPack(Path pack, Path data, Path output) throws IOException {
        Path temporary = Files.createTempFile(output.getParent(), "bedrock-pack-", ".tmp");
        try {
            CRC32 crc = new CRC32();
            MessageDigest digest = BedrockPackFiles.sha256();
            try (OutputStream client = new CheckedOutputStream(new DigestOutputStream(Files.newOutputStream(temporary), digest), crc)) {
                ZipUtils.compress(pack, client, false, false);
            }
            Files.writeString(data.resolve(BedrockPackFiles.CLIENT_SHA256), HexFormat.of().formatHex(digest.digest()));
            try (InputStream input = Files.newInputStream(temporary)) {
                ZipUtils.compressWithStoredEntry(data, output, BedrockPackFiles.CLIENT_PACK, input, Files.size(temporary), crc.getValue());
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static JsonObject manifest(String name, String description) {
        JsonArray version = new JsonArray();
        version.add(1);
        version.add(0);
        version.add(0);

        JsonArray engineVersion = new JsonArray();
        engineVersion.add(1);
        engineVersion.add(21);
        engineVersion.add(0);

        JsonObject header = new JsonObject();
        header.addProperty("name", name);
        header.addProperty("description", description);
        header.add("version", version);
        header.add("min_engine_version", engineVersion);

        JsonObject module = new JsonObject();
        module.addProperty("type", "resources");
        module.add("version", version);

        JsonArray modules = new JsonArray();
        modules.add(module);

        JsonObject manifest = new JsonObject();
        manifest.addProperty(FORMAT_VERSION, BedrockFormatVersions.MANIFEST);
        manifest.add("header", header);
        manifest.add("modules", modules);
        return manifest;
    }
}
