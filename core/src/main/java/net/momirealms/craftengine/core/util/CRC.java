package net.momirealms.craftengine.core.util;

final class CRC {
    private static final int[] crcTable = new int[256];
    private int crc = 0xffffffff;

    static {
        for (int n = 0; n < 256; n++) {
            int c = n;
            for (int k = 0; k < 8; k++) {
                if ((c & 1) == 1) {
                    c = 0xedb88320 ^ (c >>> 1);
                } else {
                    c >>>= 1;
                }

                crcTable[n] = c;
            }
        }
    }

    CRC() {}

    void reset() {
        crc = 0xffffffff;
    }

    void update(byte[] data, int off, int len) {
        int c = crc;
        for (int n = 0; n < len; n++) {
            c = crcTable[(c ^ data[off + n]) & 0xff] ^ (c >>> 8);
        }
        crc = c;
    }

    void update(int data) {
        crc = crcTable[(crc ^ data) & 0xff] ^ (crc >>> 8);
    }

    int getValue() {
        return ~crc;
    }
}