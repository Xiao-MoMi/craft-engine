package net.momirealms.craftengine.bedrock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class BedrockPackFiles {
    public static final String DATA_EXTENSION = ".cedata";
    public static final String CLIENT_PACK = "client.mcpack";
    public static final String CLIENT_SHA256 = "craftengine/client.sha256";
    public static final String CONTENT_SHA256 = "craftengine/client-content.sha256";
    public static final List<String> SERVER_FILES = List.of(
            RegistrationPlanKind.ITEM.file(),
            RegistrationPlanKind.BLOCK.file(),
            BedrockPackLayout.MODEL_DISPLAYS,
            BedrockPackLayout.ELYTRA_VARIANTS,
            BedrockPackLayout.NATIVE_MODELS,
            BedrockPackLayout.TRANSLATIONS,
            BedrockPackLayout.PLAYER_PACK,
            BedrockPackLayout.SCOREBOARD,
            BedrockPackLayout.TITLE,
            BedrockPackLayout.BOSS_BARS,
            BedrockPackLayout.FONTS
    );

    private BedrockPackFiles() {
    }

    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static String sha256(Path file) throws IOException {
        MessageDigest digest = sha256();
        try (InputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String readDigest(ZipFile archive, String name) throws IOException {
        ZipEntry entry = archive.getEntry(name);
        if (entry == null || entry.getSize() != 64) {
            throw new IOException("Missing or invalid " + name + " in " + archive.getName());
        }
        try (InputStream input = archive.getInputStream(entry)) {
            return new String(input.readNBytes(64), StandardCharsets.US_ASCII);
        }
    }

    public static Path data(Path pack, Path directory) {
        return directory.resolve(baseName(pack) + DATA_EXTENSION);
    }

    private static String baseName(Path file) {
        String name = file.getFileName().toString();
        int extension = name.lastIndexOf('.');
        return extension < 0 ? name : name.substring(0, extension);
    }
}
