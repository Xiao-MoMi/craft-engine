package net.momirealms.craftengine.core.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class SkullUtils {

    private SkullUtils() {}

    public static String identifierFromBase64(String base64) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
        int urlStartIndex = decodedString.indexOf("\"url\":\"") + 8;
        int urlEndIndex = decodedString.indexOf("\"", urlStartIndex);
        String textureUrl = decodedString.substring(urlStartIndex, urlEndIndex);
        return textureUrl.substring(textureUrl.lastIndexOf('/') + 1);
    }
}
