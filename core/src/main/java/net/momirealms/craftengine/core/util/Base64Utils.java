package net.momirealms.craftengine.core.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Utils {

    private Base64Utils() {}

    public static byte[] decode(byte[] input, int times) {
        for (int i = 0; i < times; i++) {
            input = Base64.getDecoder().decode(input);
        }
        return input;
    }

    public static String encode(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
}
