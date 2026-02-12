package net.momirealms.craftengine.core.world.chunk.client;

public final class ClientSection {
    private ClientSectionOcclusionStorage storage;

    public ClientSection(ClientSectionOcclusionStorage storage) {
        this.storage = storage;
    }

    public boolean isOccluding(int x, int y, int z) {
        return isOccluding((y << 4 | z) << 4 | x);
    }

    public boolean isOccluding(int index) {
        return this.storage.isOccluding(index);
    }

    public void setOccluding(int x, int y, int z, boolean value) {
        this.setOccluding((y << 4 | z) << 4 | x, value);
    }

    public void setOccluding(int index, boolean value) {
        boolean wasOccluding = this.storage.isOccluding(index);
        if (wasOccluding != value) {
            if (this.storage instanceof PackedOcclusionStorage arrayStorage) {
                arrayStorage.set(index, value);
            } else {
                PackedOcclusionStorage newStorage = new PackedOcclusionStorage(wasOccluding);
                newStorage.set(index, value);
                this.storage = newStorage;
            }
        }
    }
}
