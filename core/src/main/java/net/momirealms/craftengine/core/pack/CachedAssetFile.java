package net.momirealms.craftengine.core.pack;

public class CachedAssetFile {
    private final byte[] data;
    private final long lastModified;
    private final long size;

    public CachedAssetFile(byte[] data, long lastModified, long size) {
        this.data = data;
        this.lastModified = lastModified;
        this.size = size;
    }

    public byte[] data() {
        return data;
    }

    public long lastModified() {
        return lastModified;
    }

    public long size() {
        return size;
    }
}
