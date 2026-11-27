package net.momirealms.craftengine.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtils {
    private HashUtils() {}

    public static String sha1(Path filePath) {
        return sha1(filePath, new byte[8192]);
    }

    public static String sha1(Path filePath, byte[] buffer) {
        if (buffer.length == 0) {
            throw new IllegalArgumentException("Hash buffer must not be empty");
        }
        try (InputStream is = Files.newInputStream(filePath)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            int len;
            while ((len = is.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            byte[] digest = md.digest();
            return HexFormat.of().formatHex(digest);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate SHA1", e);
        }
    }

    public static String sha1(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
