package net.momirealms.craftengine.bedrock;

import java.util.UUID;

public final class FurnitureShulkerUUID {
    private static final long PREFIX = 0xce6675726e698001L;
    private static final long CUBE_PREFIX = 0xce66000000008003L;
    private static final long CUBE_MASK = 0xffff00000000ffffL;

    private FurnitureShulkerUUID() {
    }

    public static UUID create() {
        return new UUID(PREFIX, UUID.randomUUID().getLeastSignificantBits());
    }

    public static UUID createCube(float side) {
        long size = Integer.toUnsignedLong(Float.floatToIntBits(side)) << 16;
        return new UUID(CUBE_PREFIX | size, UUID.randomUUID().getLeastSignificantBits());
    }

    public static boolean isShulker(UUID uuid) {
        return uuid.getMostSignificantBits() == PREFIX;
    }

    public static boolean isCube(UUID uuid) {
        return (uuid.getMostSignificantBits() & CUBE_MASK) == CUBE_PREFIX;
    }

    public static float cubeSide(UUID uuid) {
        return Float.intBitsToFloat((int) (uuid.getMostSignificantBits() >>> 16));
    }
}
