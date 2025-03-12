package net.momirealms.craftengine.core.util;

import java.util.Base64;

public class Base64Utils {

    private Base64Utils() {}

    public static byte[] decode(byte[] input, int times) {
        for (int i = 0; i < times; i++) {
            input = Base64.getDecoder().decode(input);
        }
        return input;
    }
}
